"use client";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useCartStore } from "@/stores/useCartStore";
import { useToast } from "@/components/ui/Toast";
import { OrderSummary } from "./OrderSummary";
import { CountdownTimer } from "./CountdownTimer";
import { PaymentMethodSelector } from "./PaymentMethodSelector";
import { Shield } from "lucide-react";
import { api } from "@/lib/api";
import { generateIdempotencyKey } from "@/lib/utils";

export function CheckoutForm() {
  const router = useRouter();
  const { toast } = useToast();
  const { holdExpiresAt, paymentMethod, isProcessing, setPaymentMethod, startPayment, completePayment, cancelPayment } = useCartStore();

  // Listen for offline network drops
  useEffect(() => {
    const handleOffline = () => {
      toast({
        title: "Mất kết nối Internet",
        description: "Vui lòng kiểm tra lại đường truyền mạng của bạn.",
        variant: "error",
      });
    };
    window.addEventListener("offline", handleOffline);
    return () => window.removeEventListener("offline", handleOffline);
  }, [toast]);

  const handlePay = async () => {
    if (!paymentMethod) {
      toast({ title: "Vui lòng chọn phương thức thanh toán", variant: "error" });
      return;
    }
    startPayment();
    toast({ title: "Đang xử lý thanh toán...", description: "Vui lòng không đóng trang", variant: "default" });

    try {
      const items = useCartStore.getState().items;
      if (items.length === 0) throw new Error("Giỏ hàng trống");

      // Generate one base key, then derive per-item keys to ensure each item
      // has its own idempotency key while still being bound to this checkout session.
      const baseKey = useCartStore.getState().idempotencyKey || generateIdempotencyKey();

      let lastPaymentUrl = "";

      // Purchase each item sequentially (one category at a time)
      for (let i = 0; i < items.length; i++) {
        const item = items[i];
        const itemKey = `${baseKey}-item${i}-cat${item.categoryId}`;

        let retries = 0;
        const maxRetries = 3;
        let purchased = false;

        while (retries <= maxRetries && !purchased) {
          try {
            const response = await api.post<any>("/tickets/purchase", {
              categoryId: item.categoryId,
              quantity: item.quantity,
              idempotencyKey: itemKey,
            });

            // Use the payment URL from the last successful item (they're all the same mock gateway)
            lastPaymentUrl =
              response.paymentUrl ||
              `/payment/callback?vnp_ResponseCode=00&vnp_TxnRef=${response.id || "DEMO"}`;
            purchased = true;
          } catch (error: any) {
            const status = error.status;
            if (status === 400) {
              // Out of stock or per-user limit exceeded — abort everything
              throw new Error(
                error.message?.includes("limit")
                  ? `Bạn đã đạt giới hạn số vé cho loại "${item.name}". Vui lòng giảm số lượng.`
                  : `Rất tiếc, loại vé "${item.name}" vừa hết. Vui lòng chọn số lượng khác.`
              );
            }
            if (status >= 500 && retries < maxRetries) {
              retries++;
              // Exponential backoff: 2s, 4s, 8s
              await new Promise((resolve) => setTimeout(resolve, Math.pow(2, retries) * 1000));
              continue;
            }
            throw error;
          }
        }
      }

      // Redirect to payment gateway after all items are reserved
      window.location.href = lastPaymentUrl;
    } catch (error: any) {
      cancelPayment();
      console.error(error);
      toast({
        title: "Khởi tạo thanh toán thất bại",
        description: error.message || "Có lỗi xảy ra. Vui lòng thử lại.",
        variant: "error",
      });
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
      {/* Left: Form */}
      <div className="lg:col-span-3 space-y-6">
        <CountdownTimer expiresAt={holdExpiresAt} />
        <PaymentMethodSelector selected={paymentMethod} onSelect={setPaymentMethod} />
        <div className="flex items-center gap-2 text-sm text-muted-foreground p-3 bg-secondary rounded-xl">
          <Shield className="w-4 h-4 text-primary" />
          Thanh toán được bảo vệ bởi mã hóa SSL 256-bit
        </div>
        <button
          onClick={handlePay}
          disabled={!paymentMethod || isProcessing}
          className="w-full bg-primary text-white font-bold py-4 rounded-xl hover:bg-primary-hover transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 text-lg"
        >
          {isProcessing ? (
            <>
              <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin-slow" />
              Đang xử lý...
            </>
          ) : (
            "Thanh toán"
          )}
        </button>
      </div>
      {/* Right: Summary */}
      <div className="lg:col-span-2">
        <OrderSummary />
      </div>
    </div>
  );
}
