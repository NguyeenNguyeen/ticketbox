"use client";

import { useParams } from "next/navigation";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { ETicket } from "@/components/ticket/ETicket";
import { CheckCircle, Download, ArrowLeft } from "lucide-react";
import Link from "next/link";
import type { ETicket as ETicketType } from "@/types/order";

// Mock ticket data - in production this would come from API
const mockTicket: ETicketType = {
  id: "tkt-001",
  orderId: "ord-001",
  concertId: "concert-1",
  concertTitle: "Anh Trai Say Hi - Live Concert 2026",
  concertDate: "2026-12-20T19:30:00+07:00",
  venue: "Sân vận động Mỹ Đình, Hà Nội",
  zone: "VIP",
  row: "E",
  seatNumber: 5,
  qrCode: "TICKETBOX-TKT001-CONCERT1-VIPE5-2026",
  holderName: "Nguyễn Văn A",
  holderEmail: "nguyenvana@gmail.com",
};

export default function TicketPage() {
  const params = useParams();
  const ticketId = params.id as string;

  // In production, fetch ticket by ID
  const ticket = { ...mockTicket, id: ticketId };

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
            <button className="inline-flex items-center justify-center gap-2 bg-primary text-white font-semibold px-6 py-3 rounded-xl hover:bg-primary-hover transition-all">
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
