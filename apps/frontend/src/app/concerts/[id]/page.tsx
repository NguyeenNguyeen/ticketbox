"use client";

import { useParams } from "next/navigation";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { ConcertInfo } from "@/components/concert/ConcertInfo";
import { SeatMap } from "@/components/seatmap/SeatMap";
import { useSeatStore } from "@/stores/useSeatStore";
import { useCartStore } from "@/stores/useCartStore";
import { mockConcerts } from "@/mocks/concerts";
import { formatCurrency } from "@/lib/utils";
import { ShoppingCart, Ticket } from "lucide-react";
import Link from "next/link";
import type { OrderItem } from "@/types/order";

export default function ConcertDetailPage() {
  const params = useParams();
  const concertId = params.id as string;
  const concert = mockConcerts.find((c) => c.id === concertId);

  const selectedSeats = useSeatStore((s) => s.selectedSeats);
  const seats = useSeatStore((s) => s.seats);
  const clearSelection = useSeatStore((s) => s.clearSelection);
  const setItems = useCartStore((s) => s.setItems);

  if (!concert) {
    return (
      <>
        <Header />
        <main className="flex-1 flex items-center justify-center min-h-[60vh]">
          <div className="text-center">
            <Ticket className="w-16 h-16 text-muted-foreground mx-auto mb-4" />
            <h1 className="text-2xl font-bold mb-2">Không tìm thấy sự kiện</h1>
            <p className="text-muted-foreground mb-6">
              Sự kiện này không tồn tại hoặc đã bị xóa.
            </p>
            <Link
              href="/"
              className="inline-flex items-center gap-2 bg-primary text-white px-6 py-3 rounded-xl hover:bg-primary-hover transition-colors"
            >
              Về trang chủ
            </Link>
          </div>
        </main>
        <Footer />
      </>
    );
  }

  const selectedSeatData = selectedSeats
    .map((id) => seats[id])
    .filter(Boolean);

  const totalAmount = selectedSeatData.reduce((sum, s) => sum + s.price, 0);

  const handleProceedToCheckout = () => {
    const items: OrderItem[] = selectedSeatData.map((s) => ({
      seatId: s.id,
      zone: s.zone,
      row: s.row,
      number: s.number,
      price: s.price,
    }));

    setItems(items, concert.id, concert.title);

    // Set a hold expiry 10 minutes from now
    const holdExpiry = new Date(Date.now() + 10 * 60 * 1000).toISOString();
    useCartStore.getState().setHoldExpiry(holdExpiry);

    window.location.href = "/checkout";
  };

  return (
    <>
      <Header />
      <main className="flex-1 bg-secondary/30">
        {/* Concert Info Section */}
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <ConcertInfo concert={concert} />
        </div>

        {/* Seat Map Section */}
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-8">
          <div className="bg-white rounded-2xl shadow-sm border border-border p-6 md:p-8">
            <h2 className="text-2xl font-bold mb-6 flex items-center gap-2">
              <Ticket className="w-6 h-6 text-primary" />
              Chọn ghế
            </h2>
            <SeatMap concertId={concertId} />
          </div>
        </div>

        {/* Floating Cart Bar */}
        {selectedSeats.length > 0 && (
          <div className="fixed bottom-0 left-0 right-0 z-50 bg-white border-t border-border shadow-[0_-4px_20px_rgba(0,0,0,0.1)] animate-fade-in">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
              <div className="flex items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                  <div className="bg-primary/10 rounded-full p-2">
                    <ShoppingCart className="w-5 h-5 text-primary" />
                  </div>
                  <div>
                    <p className="font-semibold text-sm">
                      {selectedSeats.length} ghế đã chọn
                    </p>
                    <p className="text-xl font-bold text-primary">
                      {formatCurrency(totalAmount)}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <button
                    onClick={clearSelection}
                    className="px-4 py-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
                  >
                    Xóa tất cả
                  </button>
                  <button
                    onClick={handleProceedToCheckout}
                    className="bg-primary text-white font-semibold px-6 py-3 rounded-xl hover:bg-primary-hover transition-all hover:scale-105 shadow-lg shadow-primary/25"
                  >
                    Tiến hành thanh toán
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>
      <Footer />
    </>
  );
}
