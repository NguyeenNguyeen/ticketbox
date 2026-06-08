"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { CheckCircle2, XCircle, Loader2 } from "lucide-react";
import Link from "next/link";
import { useCartStore } from "@/stores/useCartStore";
import { useSeatStore } from "@/stores/useSeatStore";

export default function PaymentCallbackPage() {
  const router = useRouter();
  const [status, setStatus] = useState<"loading" | "success" | "error">("loading");
  const [errorMessage, setErrorMessage] = useState("");
  
  const clearCart = useCartStore((s) => s.clearCart);
  const clearSeats = useSeatStore((s) => s.clearSelection);

  useEffect(() => {
    // VNPAY uses vnp_ResponseCode (00 is success)
    // MoMo uses resultCode (0 is success)
    // We also support a generic 'status=success' for testing
    const searchParams = new URLSearchParams(window.location.search);
    const vnpResponse = searchParams.get("vnp_ResponseCode");
    const momoResult = searchParams.get("resultCode");
    const genericStatus = searchParams.get("status");

    // Simulate checking with backend
    const verifyPayment = async () => {
      try {
        // Just checking params for now
        const isSuccess = 
          vnpResponse === "00" || 
          momoResult === "0" || 
          genericStatus === "success";

        if (isSuccess) {
          setStatus("success");
          clearCart();
          clearSeats();
        } else {
          setStatus("error");
          setErrorMessage("Giao dịch bị từ chối hoặc đã bị huỷ bởi người dùng.");
        }
      } catch (err) {
        setStatus("error");
        setErrorMessage("Lỗi khi xác minh giao dịch với máy chủ.");
      }
    };

    if (vnpResponse || momoResult || genericStatus) {
      verifyPayment();
    } else {
      setStatus("error");
      setErrorMessage("Không tìm thấy thông tin giao dịch hợp lệ.");
    }
  }, [clearCart, clearSeats]);

  return (
    <div className="min-h-screen flex flex-col bg-secondary/30">
      <Header />
      <main className="flex-1 flex items-center justify-center p-4">
        <div className="bg-white rounded-3xl border border-border shadow-sm p-8 md:p-12 max-w-lg w-full text-center">
          {status === "loading" && (
            <div className="py-8">
              <Loader2 className="w-16 h-16 text-primary animate-spin mx-auto mb-6" />
              <h1 className="text-2xl font-bold mb-2">Đang xử lý thanh toán</h1>
              <p className="text-muted-foreground">
                Vui lòng không đóng trình duyệt trong lúc này...
              </p>
            </div>
          )}

          {status === "success" && (
            <div className="py-4 animate-fade-in">
              <div className="w-20 h-20 bg-success/10 rounded-full flex items-center justify-center mx-auto mb-6">
                <CheckCircle2 className="w-10 h-10 text-success" />
              </div>
              <h1 className="text-3xl font-bold mb-4">Thanh toán thành công!</h1>
              <p className="text-muted-foreground mb-8">
                Cảm ơn bạn đã mua vé. Vé điện tử (E-Ticket) đã được tạo và gửi đến email của bạn.
              </p>
              <div className="space-y-3">
                <Link
                  href="/orders"
                  className="block w-full bg-primary text-white font-semibold py-3.5 rounded-xl hover:bg-primary-hover transition-colors"
                >
                  Xem vé của tôi
                </Link>
                <Link
                  href="/"
                  className="block w-full bg-secondary text-foreground font-semibold py-3.5 rounded-xl hover:bg-secondary/80 transition-colors"
                >
                  Về trang chủ
                </Link>
              </div>
            </div>
          )}

          {status === "error" && (
            <div className="py-4 animate-fade-in">
              <div className="w-20 h-20 bg-destructive/10 rounded-full flex items-center justify-center mx-auto mb-6">
                <XCircle className="w-10 h-10 text-destructive" />
              </div>
              <h1 className="text-3xl font-bold mb-4">Thanh toán thất bại</h1>
              <p className="text-muted-foreground mb-8">
                {errorMessage}
              </p>
              <div className="space-y-3">
                <button
                  onClick={() => router.push("/checkout")}
                  className="block w-full bg-primary text-white font-semibold py-3.5 rounded-xl hover:bg-primary-hover transition-colors"
                >
                  Thử thanh toán lại
                </button>
                <Link
                  href="/"
                  className="block w-full bg-secondary text-foreground font-semibold py-3.5 rounded-xl hover:bg-secondary/80 transition-colors"
                >
                  Về trang chủ
                </Link>
              </div>
            </div>
          )}
        </div>
      </main>
      <Footer />
    </div>
  );
}
