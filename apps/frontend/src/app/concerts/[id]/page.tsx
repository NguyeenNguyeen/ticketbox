"use client";

import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { ConcertInfo } from "@/components/concert/ConcertInfo";
import { TicketSelector } from "@/components/concert/TicketSelector";
import { useCartStore } from "@/stores/useCartStore";
import { useAuthStore } from "@/stores/useAuthStore";
import { api } from "@/lib/api";
import { formatCurrency } from "@/lib/utils";
import { API_BASE_URL } from "@/lib/constants";
import { ShoppingCart, Ticket, Users, Map } from "lucide-react";
import Link from "next/link";
import { InteractiveSeatMap } from "@/components/concert/InteractiveSeatMap";
import { ArtistBioModal } from "@/components/concert/ArtistBioModal";
import { CaptchaModal } from "@/components/checkout/CaptchaModal";
import type { OrderItem } from "@/types/order";
import type { Concert, Artist } from "@/types/concert";

export default function ConcertDetailPage() {
  const params = useParams();
  const router = useRouter();
  const concertId = params.id as string;
  const [concert, setConcert] = useState<Concert | null>(null);
  const [loading, setLoading] = useState(true);

  const [selectedItems, setSelectedItems] = useState<OrderItem[]>([]);
  const setItems = useCartStore((s) => s.setItems);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  const [selectedArtist, setSelectedArtist] = useState<Artist | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [showCaptcha, setShowCaptcha] = useState(false);

  const handleArtistClick = (artist: Artist) => {
    setSelectedArtist(artist);
    setIsModalOpen(true);
  };

  useEffect(() => {
    async function fetchConcert() {
      try {
        setLoading(true);
        const data = await api.get<Concert>(`/concerts/${concertId}`);
        setConcert(data);
      } catch (err) {
        console.error("Failed to load concert details", err);
      } finally {
        setLoading(false);
      }
    }
    if (concertId) fetchConcert();
  }, [concertId]);

  if (loading) {
    return (
      <>
        <Header />
        <main className="flex-1 flex items-center justify-center min-h-[60vh]">
          <div className="text-center">
            <div className="w-12 h-12 border-4 border-primary/30 border-t-primary rounded-full animate-spin mx-auto mb-4" />
            <p className="text-muted-foreground">Đang tải thông tin sự kiện...</p>
          </div>
        </main>
        <Footer />
      </>
    );
  }

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

  const totalAmount = selectedItems.reduce((sum, item) => sum + (item.price * item.quantity), 0);
  const totalTickets = selectedItems.reduce((sum, item) => sum + item.quantity, 0);

  const handleCheckoutClick = () => {
    if (!isAuthenticated) {
      router.push(`/auth/login?redirect=/concerts/${concertId}`);
      return;
    }
    setShowCaptcha(true);
  };

  const handleProceedToCheckout = () => {
    setShowCaptcha(false);

    setItems(selectedItems, concert.id, concert.title);

    // Set a hold expiry 10 minutes from now
    const holdExpiry = new Date(Date.now() + 10 * 60 * 1000).toISOString();
    useCartStore.getState().setHoldExpiry(holdExpiry);

    router.push("/checkout");
  };

  return (
    <>
      <Header />
      <main className="flex-1 bg-secondary/30">
        {/* Concert Info Section */}
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <ConcertInfo concert={concert} />
          
          {/* Artists Section */}
          {concert.artists && concert.artists.length > 0 && (
            <div className="mt-8 bg-white rounded-2xl shadow-sm border border-border p-6 md:p-8">
              <h2 className="text-2xl font-bold mb-6 flex items-center gap-2">
                <Users className="w-6 h-6 text-primary" />
                Nghệ sĩ khách mời
              </h2>
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-6">
                {concert.artists.map((artist) => (
                  <button
                    key={artist.id}
                    onClick={() => handleArtistClick(artist)}
                    className="flex flex-col items-center group text-left w-full focus:outline-none"
                  >
                    <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-full flex items-center justify-center bg-primary/10 mb-3 border-4 border-transparent group-hover:border-primary/20 transition-all duration-300 shadow-md group-hover:shadow-xl transform group-hover:-translate-y-1 overflow-hidden">
                      {artist.avatarUrl ? (
                        <img src={artist.avatarUrl} alt={artist.name} className="w-full h-full object-cover" />
                      ) : (
                        <Users className="w-8 h-8 text-primary/60" />
                      )}
                    </div>
                    <h3 className="font-semibold text-gray-900 group-hover:text-primary transition-colors text-center">
                      {artist.name}
                    </h3>
                    <p className="text-xs text-muted-foreground mt-1 bg-secondary/50 px-2 py-0.5 rounded-full">
                      Xem tiểu sử
                    </p>
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Seat Map Section */}
        {concert.hasSeatMap && (
          <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 pb-8">
            <div className="bg-white rounded-2xl shadow-sm border border-border p-6 md:p-8">
              <h2 className="text-2xl font-bold mb-6 flex items-center gap-2">
                <Map className="w-6 h-6 text-primary" />
                Sơ đồ ghế
              </h2>
              <InteractiveSeatMap concertId={concert.id} />
            </div>
          </div>
        )}

        {/* Ticket Selection Section */}
        <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 pb-32">
          <div className="bg-white rounded-2xl shadow-sm border border-border p-6 md:p-8">
            <h2 className="text-2xl font-bold mb-6 flex items-center gap-2">
              <Ticket className="w-6 h-6 text-primary" />
              Chọn vé
            </h2>
            <TicketSelector concertId={concertId} isCancelled={concert.status === "CANCELLED"} onSelectionChange={setSelectedItems} />
          </div>
        </div>

        {/* Floating Cart Bar */}
        {totalTickets > 0 && (
          <div className="fixed bottom-0 left-0 right-0 z-50 bg-white border-t border-border shadow-[0_-4px_20px_rgba(0,0,0,0.1)] animate-fade-in">
            <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
              <div className="flex items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                  <div className="bg-primary/10 rounded-full p-2 hidden sm:block">
                    <ShoppingCart className="w-5 h-5 text-primary" />
                  </div>
                  <div>
                    <p className="font-semibold text-sm">
                      {totalTickets} vé đã chọn
                    </p>
                    <p className="text-xl font-bold text-primary">
                      {formatCurrency(totalAmount)}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <button
                    onClick={handleCheckoutClick}
                    className="bg-primary text-white font-semibold px-6 py-3 rounded-xl hover:bg-primary-hover transition-all hover:scale-105 shadow-lg shadow-primary/25"
                  >
                    Thanh toán
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>
      
      <CaptchaModal
        isOpen={showCaptcha}
        onSuccess={handleProceedToCheckout}
        onClose={() => setShowCaptcha(false)}
      />
      <ArtistBioModal 
        artist={selectedArtist} 
        isOpen={isModalOpen} 
        onClose={() => setIsModalOpen(false)} 
      />
      
      <Footer />
    </>
  );
}
