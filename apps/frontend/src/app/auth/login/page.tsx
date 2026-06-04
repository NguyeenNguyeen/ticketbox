"use client";

import { useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuthStore } from "@/stores/useAuthStore";
import { Eye, EyeOff, Ticket, ArrowRight } from "lucide-react";
import Link from "next/link";

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const redirect = searchParams.get("redirect") || "/";
  const login = useAuthStore((s) => s.login);
  const loginAs = useAuthStore((s) => s.loginAs);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const success = await login(email, password);
      if (success) {
        router.push(redirect);
      } else {
        setError("Email hoặc mật khẩu không đúng.");
      }
    } catch {
      setError("Đã xảy ra lỗi. Vui lòng thử lại.");
    } finally {
      setLoading(false);
    }
  };

  const handleQuickLogin = (role: "CUSTOMER" | "ORGANIZER" | "CHECKER") => {
    loginAs(role);
    router.push(redirect);
  };

  return (
    <div className="min-h-screen flex">
      {/* Left - Form */}
      <div className="flex-1 flex items-center justify-center p-8">
        <div className="w-full max-w-md">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-2 mb-8">
            <Ticket className="w-8 h-8 text-primary" />
            <span className="text-2xl font-bold text-gradient-primary">
              TicketBox
            </span>
          </Link>

          <h1 className="text-3xl font-bold mb-2">Đăng nhập</h1>
          <p className="text-muted-foreground mb-8">
            Chào mừng bạn quay lại! Vui lòng đăng nhập để tiếp tục.
          </p>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-sm font-medium mb-2">Email</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="your@email.com"
                className="w-full px-4 py-3 rounded-xl border border-border bg-white focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-2">Mật khẩu</label>
              <div className="relative">
                <input
                  type={showPassword ? "text" : "password"}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full px-4 py-3 pr-12 rounded-xl border border-border bg-white focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                >
                  {showPassword ? (
                    <EyeOff className="w-5 h-5" />
                  ) : (
                    <Eye className="w-5 h-5" />
                  )}
                </button>
              </div>
            </div>

            {error && (
              <div className="text-destructive text-sm bg-destructive/10 px-4 py-3 rounded-xl">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-primary text-white font-semibold px-6 py-3 rounded-xl hover:bg-primary-hover transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {loading ? (
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin-slow" />
              ) : (
                <>
                  Đăng nhập
                  <ArrowRight className="w-5 h-5" />
                </>
              )}
            </button>
          </form>

          <p className="text-center mt-6 text-sm text-muted-foreground">
            Chưa có tài khoản?{" "}
            <Link
              href="/auth/register"
              className="text-primary font-medium hover:underline"
            >
              Đăng ký ngay
            </Link>
          </p>

          {/* Quick login for demo */}
          <div className="mt-8 pt-8 border-t border-border">
            <p className="text-sm text-muted-foreground mb-3 text-center">
              🔑 Đăng nhập nhanh (Demo)
            </p>
            <div className="grid grid-cols-3 gap-3">
              <button
                onClick={() => handleQuickLogin("CUSTOMER")}
                className="text-sm font-medium px-3 py-2 rounded-lg bg-blue-50 text-blue-700 hover:bg-blue-100 transition-colors"
              >
                Khán giả
              </button>
              <button
                onClick={() => handleQuickLogin("ORGANIZER")}
                className="text-sm font-medium px-3 py-2 rounded-lg bg-purple-50 text-purple-700 hover:bg-purple-100 transition-colors"
              >
                Ban tổ chức
              </button>
              <button
                onClick={() => handleQuickLogin("CHECKER")}
                className="text-sm font-medium px-3 py-2 rounded-lg bg-green-50 text-green-700 hover:bg-green-100 transition-colors"
              >
                Soát vé
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Right - Visual */}
      <div className="hidden lg:flex flex-1 items-center justify-center bg-gradient-to-br from-[hsl(250,84%,54%)] via-[hsl(270,70%,55%)] to-[hsl(290,60%,50%)] text-white p-12">
        <div className="max-w-md text-center">
          <div className="text-6xl mb-6">🎵</div>
          <h2 className="text-3xl font-bold mb-4">
            Trải nghiệm âm nhạc không giới hạn
          </h2>
          <p className="text-white/80 text-lg">
            Đặt vé concert dễ dàng, thanh toán an toàn, nhận e-ticket ngay lập tức với mã QR.
          </p>
        </div>
      </div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense>
      <LoginForm />
    </Suspense>
  );
}
