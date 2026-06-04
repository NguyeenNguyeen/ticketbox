"use client";
import { useRouter } from "next/navigation";
import { useCartStore } from "@/stores/useCartStore";
import { useToast } from "@/components/ui/Toast";
import { OrderSummary } from "./OrderSummary";
import { CountdownTimer } from "./CountdownTimer";
import { PaymentMethodSelector } from "./PaymentMethodSelector";
import { Shield } from "lucide-react";

export function CheckoutForm() {
  const router = useRouter();
  const { toast } = useToast();
  const { holdExpiresAt, paymentMethod, isProcessing, setPaymentMethod, startPayment, completePayment } = useCartStore();

  const handlePay = async () => {
    if (!paymentMethod) { toast({ title: "Vui lòng chọn phương thức thanh toán", variant: "error" }); return; }
    startPayment();
    toast({ title: "Đang xử lý thanh toán...", description: "Vui lòng không đóng trang", variant: "default" });

    // Simulate payment
    await new Promise((r) => setTimeout(r, 2500));
    completePayment();
    toast({ title: "Thanh toán thành công! 🎉", variant: "success" });
    router.push("/tickets/tkt-001");
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
