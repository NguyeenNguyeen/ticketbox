"use client";

import { useEffect, useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { api } from "@/lib/api";
import { useToast } from "@/components/ui/Toast";
import { CreditCard, Smartphone, Check, ArrowRight, ShieldCheck, AlertCircle, RefreshCw, XCircle, Info, Lock } from "lucide-react";
import { formatCurrency } from "@/lib/utils";

function SandboxForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { toast } = useToast();

  const orderIdStr = searchParams.get("orderId");
  const provider = searchParams.get("provider") || "VNPAY";
  const orderId = orderIdStr ? parseInt(orderIdStr) : null;

  const [loading, setLoading] = useState(false);
  const [amount, setAmount] = useState<number | null>(null);

  // VNPAY form states
  const [cardNumber, setCardNumber] = useState("9704198526191432");
  const [cardHolder, setCardHolder] = useState("NGUYEN VAN A");
  const [expiryDate, setExpiryDate] = useState("07/15");
  const [otp, setOtp] = useState("123456");
  const [showOtp, setShowOtp] = useState(false);

  // Load order details from history
  useEffect(() => {
    if (!orderId) return;
    const fetchOrderAmount = async () => {
      try {
        const orders = await api.get<any[]>("/orders/history");
        const matched = orders.find((o) => o.id === orderIdStr);
        if (matched) {
          setAmount(matched.totalAmount);
        }
      } catch (err) {
        console.error("Failed to load order amount from history", err);
      }
    };
    fetchOrderAmount();
  }, [orderId, orderIdStr]);

  const handleComplete = async (status: "SUCCESS" | "FAILED") => {
    if (!orderId) {
      toast({ title: "Mã đơn hàng không hợp lệ", variant: "error" });
      return;
    }
    setLoading(true);

    try {
      await api.post("/payments/sandbox/complete", {
        orderId,
        status,
        provider,
      });

      if (status === "SUCCESS") {
        toast({ title: "Thanh toán thành công!", variant: "success" });
        if (provider === "VNPAY") {
          router.push(`/payment/callback?vnp_ResponseCode=00&vnp_TxnRef=TX-${orderId}`);
        } else {
          router.push(`/payment/callback?resultCode=0&orderId=${orderId}`);
        }
      } else {
        toast({ title: "Đã hủy giao dịch", variant: "default" });
        if (provider === "VNPAY") {
          router.push(`/payment/callback?vnp_ResponseCode=24&vnp_TxnRef=TX-${orderId}`);
        } else {
          router.push(`/payment/callback?resultCode=49&orderId=${orderId}`);
        }
      }
    } catch (err: any) {
      console.error(err);
      toast({
        title: "Lỗi kết nối Sandbox",
        description: err.message || "Không thể gửi trạng thái thanh toán đến máy chủ.",
        variant: "error",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleVnpayPay = (e: React.FormEvent) => {
    e.preventDefault();
    if (!showOtp) {
      setShowOtp(true);
      toast({ title: "Đã gửi mã OTP giả lập: 123456", variant: "default" });
    } else {
      if (otp !== "123456") {
        toast({ title: "Mã OTP không đúng (Mặc định: 123456)", variant: "error" });
        return;
      }
      handleComplete("SUCCESS");
    }
  };

  const displayAmount = amount !== null ? formatCurrency(amount) : "...";

  return (
    <div className="max-w-2xl mx-auto p-4 sm:p-6 lg:p-8">
      {/* Sandbox Alert Header */}
      <div className="flex items-center gap-3 p-4 mb-6 rounded-2xl bg-amber-50 border border-amber-200 text-amber-800 text-sm">
        <AlertCircle className="w-5 h-5 flex-shrink-0 text-amber-600 animate-pulse" />
        <div>
          <span className="font-bold">Chế độ giả lập (Sandbox Mode):</span> Đây là môi trường thử nghiệm thanh toán nội bộ. KHÔNG sử dụng thông tin thẻ ngân hàng thật của bạn.
        </div>
      </div>

      <div className="bg-white rounded-3xl border border-border shadow-sm overflow-hidden">
        {provider === "VNPAY" ? (
          /* ========================================================================= */
          /* VNPAY SANDBOX INTERFACE                                                   */
          /* ========================================================================= */
          <div>
            {/* Header */}
            <div className="bg-gradient-to-r from-blue-600 to-sky-500 p-6 text-white text-center">
              <div className="inline-flex items-center gap-2 px-3 py-1 bg-white/20 rounded-full text-xs font-semibold mb-2 backdrop-blur-sm">
                <CreditCard className="w-3.5 h-3.5" /> VNPAY GATEWAY
              </div>
              <h2 className="text-2xl font-black tracking-tight">VNPAY Sandbox Gateway</h2>
              <p className="text-white/80 text-xs mt-1">Cổng thanh toán giả lập ngân hàng NCB</p>
            </div>

            {/* Content */}
            <div className="p-6 space-y-6">
              {/* Order Info Bar */}
              <div className="flex justify-between items-center p-4 bg-secondary rounded-2xl">
                <div>
                  <p className="text-xs text-muted-foreground">Mã đơn hàng</p>
                  <p className="font-bold text-foreground">#{orderIdStr}</p>
                </div>
                <div className="text-right">
                  <p className="text-xs text-muted-foreground">Số tiền thanh toán</p>
                  <p className="text-xl font-extrabold text-blue-600">{displayAmount}</p>
                </div>
              </div>

              {/* Demo Helper Card */}
              <div className="p-4 rounded-2xl bg-blue-50 border border-blue-100 space-y-2 text-xs text-blue-800">
                <div className="flex items-center gap-1.5 font-bold">
                  <Info className="w-4 h-4 text-blue-600" /> Thông tin thẻ test khuyến nghị:
                </div>
                <div className="grid grid-cols-2 gap-y-1 pl-5 list-disc font-medium text-blue-900">
                  <div>• Số thẻ: <span className="font-bold select-all">9704198526191432</span></div>
                  <div>• Tên chủ thẻ: <span className="font-bold select-all">NGUYEN VAN A</span></div>
                  <div>• Ngày phát hành: <span className="font-bold select-all">07/15</span></div>
                  <div>• Mã OTP: <span className="font-bold select-all">123456</span></div>
                </div>
              </div>

              {/* Form */}
              <form onSubmit={handleVnpayPay} className="space-y-4">
                {!showOtp ? (
                  <>
                    <div>
                      <label className="block text-xs font-bold text-muted-foreground mb-1.5 uppercase tracking-wider">Số thẻ ngân hàng</label>
                      <input
                        type="text"
                        value={cardNumber}
                        onChange={(e) => setCardNumber(e.target.value)}
                        className="w-full px-4 py-3 rounded-xl border border-border bg-white text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                        required
                      />
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="block text-xs font-bold text-muted-foreground mb-1.5 uppercase tracking-wider">Ngày phát hành (MM/YY)</label>
                        <input
                          type="text"
                          value={expiryDate}
                          onChange={(e) => setExpiryDate(e.target.value)}
                          className="w-full px-4 py-3 rounded-xl border border-border bg-white text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all text-center"
                          required
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-bold text-muted-foreground mb-1.5 uppercase tracking-wider">Tên chủ thẻ (Không dấu)</label>
                        <input
                          type="text"
                          value={cardHolder}
                          onChange={(e) => setCardHolder(e.target.value.toUpperCase())}
                          className="w-full px-4 py-3 rounded-xl border border-border bg-white text-sm font-bold focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all uppercase"
                          required
                        />
                      </div>
                    </div>
                  </>
                ) : (
                  <div className="animate-fade-in space-y-4">
                    <div className="text-center p-4 bg-blue-50/50 rounded-2xl border border-dashed border-blue-200">
                      <Lock className="w-8 h-8 text-blue-600 mx-auto mb-2 animate-bounce" />
                      <p className="text-sm font-semibold">Xác thực mã OTP</p>
                      <p className="text-xs text-muted-foreground mt-1">Một mã OTP giả lập đã được gửi đến số điện thoại liên kết của bạn.</p>
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-muted-foreground mb-1.5 uppercase tracking-wider text-center">Nhập mã OTP (Mặc định: 123456)</label>
                      <input
                        type="text"
                        value={otp}
                        onChange={(e) => setOtp(e.target.value)}
                        className="w-32 mx-auto px-4 py-3 rounded-xl border border-border bg-white text-lg font-black tracking-widest text-center focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                        maxLength={6}
                        required
                      />
                    </div>
                  </div>
                )}

                {/* Actions */}
                <div className="pt-4 space-y-3">
                  <button
                    type="submit"
                    disabled={loading}
                    className="w-full bg-blue-600 text-white font-bold py-4 rounded-xl hover:bg-blue-700 transition-all disabled:opacity-50 flex items-center justify-center gap-2 text-base shadow-lg shadow-blue-500/25"
                  >
                    {loading ? (
                      <RefreshCw className="w-5 h-5 animate-spin" />
                    ) : showOtp ? (
                      <>Xác nhận giao dịch <Check className="w-5 h-5" /></>
                    ) : (
                      <>Tiếp tục thanh toán <ArrowRight className="w-5 h-5" /></>
                    )}
                  </button>

                  <button
                    type="button"
                    onClick={() => handleComplete("FAILED")}
                    disabled={loading}
                    className="w-full bg-secondary hover:bg-secondary/80 text-foreground font-semibold py-3.5 rounded-xl transition-all disabled:opacity-50 text-sm flex items-center justify-center gap-1.5"
                  >
                    <XCircle className="w-4 h-4 text-muted-foreground" /> Hủy bỏ thanh toán
                  </button>
                </div>
              </form>
            </div>
          </div>
        ) : (
          /* ========================================================================= */
          /* MOMO SANDBOX INTERFACE                                                    */
          /* ========================================================================= */
          <div>
            {/* Header */}
            <div className="bg-gradient-to-r from-pink-600 to-rose-500 p-6 text-white text-center">
              <div className="inline-flex items-center gap-2 px-3 py-1 bg-white/20 rounded-full text-xs font-semibold mb-2 backdrop-blur-sm">
                <Smartphone className="w-3.5 h-3.5" /> MOMO APP SCAN
              </div>
              <h2 className="text-2xl font-black tracking-tight">MoMo Sandbox QR Code</h2>
              <p className="text-white/80 text-xs mt-1">Cổng quét mã thanh toán ví MoMo giả lập</p>
            </div>

            {/* Content */}
            <div className="p-6 space-y-6">
              {/* Order Info Bar */}
              <div className="flex justify-between items-center p-4 bg-secondary rounded-2xl">
                <div>
                  <p className="text-xs text-muted-foreground">Mã đơn hàng</p>
                  <p className="font-bold text-foreground">#{orderIdStr}</p>
                </div>
                <div className="text-right">
                  <p className="text-xs text-muted-foreground">Số tiền thanh toán</p>
                  <p className="text-xl font-extrabold text-pink-600">{displayAmount}</p>
                </div>
              </div>

              {/* QR Code Graphic Mock */}
              <div className="relative w-56 h-56 mx-auto bg-white border-2 border-pink-100 rounded-3xl p-4 flex flex-col items-center justify-center shadow-inner overflow-hidden group">
                {/* Scanner Laser Animation */}
                <div className="absolute left-0 w-full h-1 bg-gradient-to-r from-transparent via-pink-500 to-transparent top-0 animate-[scan_2s_ease-in-out_infinite]" />
                
                {/* Mock QR Content */}
                <div className="grid grid-cols-4 gap-2 w-full h-full opacity-80">
                  {Array.from({ length: 16 }).map((_, idx) => {
                    const active = (idx * 7) % 3 === 0 || idx === 0 || idx === 3 || idx === 12 || idx === 15;
                    return (
                      <div
                        key={idx}
                        className={`rounded ${
                          active ? "bg-gradient-to-br from-pink-600 to-rose-600" : "bg-pink-50"
                        } transition-colors duration-500`}
                      />
                    );
                  })}
                </div>
                <div className="absolute inset-0 flex items-center justify-center">
                  <div className="w-12 h-12 bg-white rounded-2xl flex items-center justify-center shadow border border-pink-100">
                    <span className="font-black text-pink-600 text-sm">MoMo</span>
                  </div>
                </div>
              </div>

              <div className="text-center space-y-1.5 max-w-sm mx-auto">
                <p className="text-sm font-semibold">Hướng dẫn thanh toán</p>
                <p className="text-xs text-muted-foreground">
                  Mở ví MoMo quét mã QR ở trên hoặc click nút **Xác nhận thanh toán** để hoàn tất giả lập đặt vé.
                </p>
              </div>

              {/* Actions */}
              <div className="pt-4 space-y-3">
                <button
                  onClick={() => handleComplete("SUCCESS")}
                  disabled={loading}
                  className="w-full bg-pink-600 text-white font-bold py-4 rounded-xl hover:bg-pink-700 transition-all disabled:opacity-50 flex items-center justify-center gap-2 text-base shadow-lg shadow-pink-500/25"
                >
                  {loading ? (
                    <RefreshCw className="w-5 h-5 animate-spin" />
                  ) : (
                    <>Xác nhận đã quét & thanh toán <Check className="w-5 h-5" /></>
                  )}
                </button>

                <button
                  onClick={() => handleComplete("FAILED")}
                  disabled={loading}
                  className="w-full bg-secondary hover:bg-secondary/80 text-foreground font-semibold py-3.5 rounded-xl transition-all disabled:opacity-50 text-sm flex items-center justify-center gap-1.5"
                >
                  <XCircle className="w-4 h-4 text-muted-foreground" /> Hủy bỏ thanh toán
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Secure badge footer */}
        <div className="bg-secondary/30 border-t border-border p-4 flex items-center justify-center gap-2 text-xs text-muted-foreground">
          <ShieldCheck className="w-4 h-4 text-emerald-600" /> Kết nối được bảo mật bằng mã hóa SSL 256-bit
        </div>
      </div>
    </div>
  );
}

export default function SandboxPage() {
  return (
    <div className="min-h-screen flex flex-col bg-secondary/30">
      <Header />
      <main className="flex-grow flex items-center justify-center py-12">
        <Suspense fallback={
          <div className="flex items-center gap-2">
            <RefreshCw className="w-6 h-6 animate-spin text-primary" />
            <span>Đang tải Sandbox...</span>
          </div>
        }>
          <SandboxForm />
        </Suspense>
      </main>
      <Footer />
    </div>
  );
}
