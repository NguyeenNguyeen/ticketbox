"use client";
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

  const handlePay = async () => {
    if (!paymentMethod) { toast({ title: "Vui lòng chọn phương thức thanh toán", variant: "error" }); return; }
    startPayment();
    toast({ title: "Đang xử lý thanh toán...", description: "Vui lòng không đóng trang", variant: "default" });

    try {
      const concertId = useCartStore.getState().concertId;
      const items = useCartStore.getState().items;

      const categories = await api.get<any[]>(`/concerts/${concertId}/categories`);

      const zoneCounts = new Map<string, number>();
      for (const item of items) {
        zoneCounts.set(item.zone, (zoneCounts.get(item.zone) || 0) + 1);
      }

      const baseKey = useCartStore.getState().idempotencyKey || generateIdempotencyKey();
      let lastOrder: any = null;

      for (const [zone, quantity] of zoneCounts.entries()) {
        const category = categories.find((c) => c.name.toUpperCase() === zone.toUpperCase());
        if (!category) {
          throw new Error(`Không tìm thấy hạng vé cho khu vực ${zone}`);
        }

        const key = zoneCounts.size > 1 ? `${baseKey}-${category.id}` : baseKey;

        const order = await api.post<any>("/tickets/purchase", {
          categoryId: category.id,
          quantity,
          idempotencyKey: key,
        });

        lastOrder = order;
      }

      completePayment();
      toast({ title: "Thanh toán thành công! 🎉", variant: "success" });

      if (lastOrder && lastOrder.id) {
        const tickets = await api.get<any[]>(`/tickets/order/${lastOrder.id}`);
        if (tickets && tickets.length > 0) {
          router.push(`/tickets/${tickets[0].id}`);
        } else {
          router.push("/");
        }
      } else {
        router.push("/");
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
