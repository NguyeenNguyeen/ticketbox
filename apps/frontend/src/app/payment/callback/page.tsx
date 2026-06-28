"use client";

import { useEffect, useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { CheckCircle2, XCircle, Loader2, Ticket } from "lucide-react";
import Link from "next/link";
import { useCartStore } from "@/stores/useCartStore";
import { api } from "@/lib/api";
import { ETicket } from "@/components/ticket/ETicket";
import type { ETicket as ETicketType } from "@/types/order";

function PaymentCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [status, setStatus] = useState<"loading" | "success" | "error">("loading");
  const [errorMessage, setErrorMessage] = useState("");
  const [tickets, setTickets] = useState<ETicketType[]>([]);
  
  const clearCart = useCartStore((s) => s.clearCart);
  const cancelPayment = useCartStore((s) => s.cancelPayment);

  useEffect(() => {
    // VNPAY uses vnp_ResponseCode (00 is success)
    // MoMo uses resultCode (0 is success)
    // We also support a generic 'status=success' for testing
    const vnpResponse = searchParams.get("vnp_ResponseCode");
    const vnpTxnRef = searchParams.get("vnp_TxnRef");
    const momoResult = searchParams.get("resultCode");
    const momoOrderId = searchParams.get("orderId");
    const genericStatus = searchParams.get("status");

    let extractedOrderId = momoOrderId;
    if (vnpTxnRef && vnpTxnRef.startsWith("TX-")) {
      extractedOrderId = vnpTxnRef.replace("TX-", "");
    }

    const verifyPayment = async () => {
      try {
        const isSuccess = 
          vnpResponse === "00" || 
          momoResult === "0" || 
          genericStatus === "success";

        if (isSuccess) {
          setStatus("success");
          clearCart();
          
          if (extractedOrderId) {
            try {
              const fetchedTickets = await api.get<ETicketType[]>(`/tickets/order/${extractedOrderId}`);
              setTickets(fetchedTickets || []);
            } catch (err) {
              console.error("Failed to fetch tickets", err);
            }
          }
        } else {
          setStatus("error");
          setErrorMessage("Giao dịch bị từ chối hoặc đã bị huỷ bởi người dùng.");
          cancelPayment();
        }
      } catch (err) {
        setStatus("error");
        setErrorMessage("Lỗi khi xác minh giao dịch với máy chủ.");
        cancelPayment();
      }
    };

    if (vnpResponse || momoResult || genericStatus) {
      verifyPayment();
    } else {
      setStatus("error");
      setErrorMessage("Không tìm thấy thông tin giao dịch hợp lệ.");
      cancelPayment();
    }
  }, [searchParams, clearCart, cancelPayment]);

  return (
    <div className="min-h-screen flex flex-col bg-secondary/30">
      <Header />
      <main className="flex-1 flex items-center justify-center p-4">
        <div className={`bg-white rounded-3xl border border-border shadow-sm p-8 md:p-12 w-full text-center ${status === "success" && tickets.length > 0 ? "max-w-4xl" : "max-w-lg"}`}>
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
              
              {tickets.length > 0 ? (
                <div className="mb-8">
                  <div className="flex items-center justify-center gap-2 mb-6">
                    <Ticket className="w-5 h-5 text-primary" />
                    <h2 className="text-xl font-semibold">Vé điện tử của bạn</h2>
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-2 gap-6 text-left">
                    {tickets.map(ticket => (
                      <ETicket key={ticket.id} ticket={ticket} />
                    ))}
                  </div>
                </div>
              ) : (
                <div className="py-6 mb-8 bg-secondary/50 rounded-xl flex items-center justify-center gap-3">
                  <Loader2 className="w-5 h-5 text-primary animate-spin" />
                  <span className="text-sm font-medium text-muted-foreground">Đang lấy thông tin vé...</span>
                </div>
              )}

              <div className="space-y-3 max-w-sm mx-auto">
                <Link
                  href="/orders"
                  className="block w-full bg-primary text-white font-semibold py-3.5 rounded-xl hover:bg-primary-hover transition-colors"
                >
                  Xem lịch sử mua hàng
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
                  onClick={() => {
                    cancelPayment();
                    router.push("/checkout");
                  }}
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

export default function PaymentCallbackPage() {
  return (
    <Suspense fallback={<div className="min-h-screen flex items-center justify-center"><Loader2 className="w-10 h-10 animate-spin text-primary" /></div>}>
      <PaymentCallbackContent />
    </Suspense>
  );
}
