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
    if (!paymentMethod) { toast({ title: "Vui lòng chọn phương thức thanh toán", variant: "error" }); return; }
    startPayment();
    toast({ title: "Đang xử lý thanh toán...", description: "Vui lòng không đóng trang", variant: "default" });

    try {
      const concertId = useCartStore.getState().concertId;
      const items = useCartStore.getState().items;

      const categories = await api.get<any[]>(`/concerts/${concertId}/categories`);

      const baseKey = useCartStore.getState().idempotencyKey || generateIdempotencyKey();
      let lastOrder: any = null;

      for (const item of items) {
        const categoryId = item.categoryId;
        const quantity = item.quantity;
        
        const category = categories.find((c) => c.id === categoryId);
        if (!category) {
          throw new Error(`Không tìm thấy hạng vé: ${item.name}`);
        }

        const key = items.length > 1 ? `${baseKey}-${categoryId}` : baseKey;

        // Exponential backoff retry logic
        let order = null;
        let retries = 0;
        const maxRetries = 3;

        while (retries <= maxRetries) {
          try {
            order = await api.post<any>("/tickets/reserve", {
              categoryId: category.id,
              quantity,
              idempotencyKey: key,
            }, { "Idempotency-Key": key });
            break; // Success, exit retry loop
          } catch (error: any) {
            const status = error.status;
            const backendMessage = error.message || "Có lỗi xảy ra. Vui lòng thử lại.";

            if (status === 400) {
              const lowStockPattern = /vé này vừa được mua mất|not enough tickets available|oversell prevented/i;
              if (lowStockPattern.test(backendMessage)) {
                throw new Error("Rất tiếc, loại vé này vừa được mua mất ở giây cuối cùng. Vui lòng chọn ghế khác.");
              }
              // Show the actual backend message for other business failures.
              throw new Error(backendMessage);
            }

            if (status >= 500 && retries < maxRetries) {
              retries++;
              const delay = Math.pow(2, retries) * 1000; // 2s, 4s, 8s
              console.warn(`Reservation failed (5xx), retrying in ${delay}ms... (Attempt ${retries}/${maxRetries})`);
              await new Promise(resolve => setTimeout(resolve, delay));
              continue;
            }

            if (status >= 500 && retries === maxRetries) {
              throw new Error("Hệ thống đặt vé đang quá tải. Vui lòng thử lại sau.");
            }

            throw error;
          }
        }

        lastOrder = order;
      }

      if (lastOrder && lastOrder.id) {
        router.push(`/payment/sandbox?orderId=${lastOrder.id}&provider=${paymentMethod}`);
      } else {
        throw new Error("Không thể khởi tạo giữ ghế.");
      }
    } catch (error: any) {
      cancelPayment();
      console.error(error);
      toast({
        title: "Thanh toán thất bại",
        description: error.message || "Có lỗi xảy ra trong quá trình đặt vé. Vui lòng kiểm tra lại.",
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
