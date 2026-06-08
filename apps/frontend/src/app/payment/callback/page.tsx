"use client";

import { useEffect, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { CheckCircle2, XCircle, Loader2 } from "lucide-react";
import Link from "next/link";
import { useCartStore } from "@/stores/useCartStore";
import { api } from "@/lib/api";

export default function PaymentCallbackPage() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const [status, setStatus] = useState<"loading" | "success" | "error">("loading");
  const [errorMessage, setErrorMessage] = useState("");
  const [orderId, setOrderId] = useState<string | null>(null);

  const clearCart = useCartStore((s) => s.clearCart);

  useEffect(() => {
    // VNPAY uses vnp_ResponseCode (00 is success)
    // MoMo uses resultCode (0 is success)
    // Demo uses generic 'status=success'
    const vnpResponse = searchParams.get("vnp_ResponseCode");
    const vnpTxnRef = searchParams.get("vnp_TxnRef");
    const momoResult = searchParams.get("resultCode");
    const genericStatus = searchParams.get("status");

    const verifyPayment = async () => {
      try {
        const isSuccess =
          vnpResponse === "00" ||
          momoResult === "0" ||
          genericStatus === "success";

        if (isSuccess) {
          // Notify backend that payment was confirmed so it can finalize the order
          // (In production this would be a server-to-server webhook, but for demo
          // we call the verify endpoint from the client with the transaction ref)
          try {
            await api.post<any>("/payments/verify", {
              provider: vnpResponse !== null ? "VNPAY" : "MOMO",
              transactionRef: vnpTxnRef || searchParams.get("transactionId") || "DEMO",
              responseCode: vnpResponse || momoResult || "00",
            });
          } catch (verifyErr) {
            // Non-fatal: payment was already finalized server-side during purchase
            console.warn("Payment verify call failed (non-fatal):", verifyErr);
          }

          setOrderId(vnpTxnRef);
          setStatus("success");
          clearCart();
        } else {
          setStatus("error");
          const codeMap: Record<string, string> = {
            "24": "Khách hàng hủy giao dịch.",
            "11": "Đã hết hạn thanh toán.",
            "12": "Thẻ/Tài khoản bị khóa.",
            "75": "Ngân hàng đang bảo trì. Vui lòng thử lại sau.",
          };
          const code = vnpResponse || momoResult || "";
          setErrorMessage(
            codeMap[code] || "Giao dịch bị từ chối hoặc đã bị huỷ bởi người dùng."
          );
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
  }, [searchParams, clearCart]);

  return (
    <div className="min-h-screen flex flex-col bg-secondary/30">
      <Header />
      <main className="flex-1 flex items-center justify-center p-4">
        <div className="bg-white rounded-3xl border border-border shadow-sm p-8 md:p-12 max-w-lg w-full text-center">
          {status === "loading" && (
            <div className="py-8">
              <Loader2 className="w-16 h-16 text-primary animate-spin mx-auto mb-6" />
              <h1 className="text-2xl font-bold mb-2">Đang xác nhận thanh toán</h1>
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
              <p className="text-muted-foreground mb-8">{errorMessage}</p>
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
