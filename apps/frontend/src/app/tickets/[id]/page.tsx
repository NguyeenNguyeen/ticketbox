"use client";

import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { ETicket } from "@/components/ticket/ETicket";
import { CheckCircle, Download, ArrowLeft, Ticket as TicketIcon } from "lucide-react";
import Link from "next/link";
import { api } from "@/lib/api";
import type { ETicket as ETicketType } from "@/types/order";

export default function TicketPage() {
  const params = useParams();
  const ticketId = params.id as string;
  const [ticket, setTicket] = useState<ETicketType | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchTicket() {
      try {
        setLoading(true);
        const data = await api.get<ETicketType>(`/tickets/${ticketId}`);
        setTicket(data);
      } catch (err) {
        console.error("Failed to fetch ticket", err);
      } finally {
        setLoading(false);
      }
    }
    if (ticketId) fetchTicket();
  }, [ticketId]);

  if (loading) {
    return (
      <>
        <Header />
        <main className="flex-1 flex items-center justify-center min-h-[60vh]">
          <div className="text-center">
            <div className="w-12 h-12 border-4 border-primary/30 border-t-primary rounded-full animate-spin mx-auto mb-4" />
            <p className="text-muted-foreground">Đang tải thông tin vé...</p>
          </div>
        </main>
        <Footer />
      </>
    );
  }

  if (!ticket) {
    return (
      <>
        <Header />
        <main className="flex-1 flex items-center justify-center min-h-[60vh]">
          <div className="text-center">
            <TicketIcon className="w-16 h-16 text-muted-foreground mx-auto mb-4" />
            <h1 className="text-2xl font-bold mb-2">Không tìm thấy vé</h1>
            <p className="text-muted-foreground mb-6">
              Vé này không tồn tại hoặc bạn không có quyền truy cập.
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

  return (
    <>
      <Header />
      <main className="flex-1 bg-secondary/30">
        <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          {/* Success message */}
          <div className="text-center mb-8 animate-fade-in-up">
            <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-success/10 mb-4">
              <CheckCircle className="w-8 h-8 text-success" />
            </div>
            <h1 className="text-3xl font-bold mb-2">Mua vé thành công! 🎉</h1>
            <p className="text-muted-foreground">
              Vé điện tử của bạn đã sẵn sàng. Hãy xuất trình mã QR tại cổng vào sự kiện.
            </p>
          </div>

          {/* E-Ticket */}
          <div className="animate-fade-in-up" style={{ animationDelay: "0.1s" }}>
            <ETicket ticket={ticket} />
          </div>

          {/* Actions */}
          <div className="flex flex-col sm:flex-row gap-4 mt-8 justify-center animate-fade-in-up" style={{ animationDelay: "0.2s" }}>
            <button 
              onClick={() => window.print()}
              className="inline-flex items-center justify-center gap-2 bg-primary text-white font-semibold px-6 py-3 rounded-xl hover:bg-primary-hover transition-all"
            >
              <Download className="w-5 h-5" />
              Tải vé về máy
            </button>
            <Link
              href="/"
              className="inline-flex items-center justify-center gap-2 bg-white text-foreground font-semibold px-6 py-3 rounded-xl border border-border hover:bg-secondary transition-all"
            >
              <ArrowLeft className="w-5 h-5" />
              Về trang chủ
            </Link>
          </div>
        </div>
      </main>
      <Footer />
    </>
  );
}
