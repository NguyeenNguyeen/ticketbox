"use client";

import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { CheckoutForm } from "@/components/checkout/CheckoutForm";
import { useCartStore } from "@/stores/useCartStore";
import { useAuthStore } from "@/stores/useAuthStore";
import { ShoppingBag } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

export default function CheckoutPage() {
  const items = useCartStore((s) => s.items);
  const concertTitle = useCartStore((s) => s.concertTitle);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const router = useRouter();

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/auth/login?redirect=/checkout");
    }
  }, [isAuthenticated, router]);

  if (!isAuthenticated) return null;

  if (items.length === 0) {
    return (
      <>
        <Header />
        <main className="flex-1 flex items-center justify-center min-h-[60vh]">
          <div className="text-center">
            <ShoppingBag className="w-16 h-16 text-muted-foreground mx-auto mb-4" />
            <h1 className="text-2xl font-bold mb-2">Giỏ vé trống</h1>
            <p className="text-muted-foreground mb-6">
              Bạn chưa chọn ghế nào. Hãy chọn ghế từ sơ đồ chỗ ngồi.
            </p>
            <Link
              href="/"
              className="inline-flex items-center gap-2 bg-primary text-white px-6 py-3 rounded-xl hover:bg-primary-hover transition-colors"
            >
              Khám phá sự kiện
            </Link>
          </div>
        </main>
        <Footer />
      </>
    );
  }

  return (
    <>
      <Header />
      <main className="flex-1 bg-secondary/30">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {/* Breadcrumb */}
          <nav className="flex items-center gap-2 text-sm text-muted-foreground mb-6">
            <Link href="/" className="hover:text-primary transition-colors">
              Trang chủ
            </Link>
            <span>/</span>
            <Link href={`/concerts/${useCartStore.getState().concertId}`} className="hover:text-primary transition-colors">
              {concertTitle}
            </Link>
            <span>/</span>
            <span className="text-foreground font-medium">Thanh toán</span>
          </nav>

          <h1 className="text-3xl font-bold mb-8">Thanh toán</h1>

          <CheckoutForm />
        </div>
      </main>
      <Footer />
    </>
  );
}
