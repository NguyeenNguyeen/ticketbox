"use client";
import { QRCodeSVG } from "qrcode.react";
import { Calendar, MapPin, User } from "lucide-react";
import { formatDate } from "@/lib/utils";
import type { ETicket as ETicketType } from "@/types/order";

export function ETicket({ ticket }: { ticket: ETicketType }) {
  return (
    <div className="bg-white rounded-2xl border border-border shadow-lg overflow-hidden max-w-md mx-auto">
      {/* Header */}
      <div className="bg-gradient-to-r from-[hsl(250,84%,54%)] to-[hsl(270,70%,55%)] p-6 text-white text-center">
        <p className="text-sm opacity-80">E-TICKET</p>
        <h2 className="text-xl font-bold mt-1">{ticket.concertTitle}</h2>
      </div>

      {/* Details */}
      <div className="p-6 space-y-3">
        <div className="flex items-center gap-2 text-sm">
          <Calendar className="w-4 h-4 text-primary" />
          <span>{formatDate(ticket.concertDate)}</span>
        </div>
        <div className="flex items-center gap-2 text-sm">
          <MapPin className="w-4 h-4 text-primary" />
          <span>{ticket.venue}</span>
        </div>
        <div className="flex items-center gap-2 text-sm">
          <User className="w-4 h-4 text-primary" />
          <span>{ticket.holderName}</span>
        </div>
      </div>

      {/* Perforation */}
      <div className="relative h-5">
        <div className="absolute inset-x-0 top-1/2 border-t-2 border-dashed border-border" />
        <div className="absolute left-0 top-1/2 -translate-y-1/2 -translate-x-1/2 w-5 h-5 rounded-full bg-secondary" />
        <div className="absolute right-0 top-1/2 -translate-y-1/2 translate-x-1/2 w-5 h-5 rounded-full bg-secondary" />
      </div>

      {/* QR Code */}
      <div className="p-6 flex flex-col items-center">
        <QRCodeSVG value={ticket.qrCode} size={180} level="H"
          bgColor="white" fgColor="hsl(250,84%,54%)" includeMargin={false} />
        <p className="text-xs text-muted-foreground mt-3 font-mono">{ticket.qrCode}</p>
      </div>

      {/* Seat info */}
      <div className="bg-secondary/50 p-4 text-center">
        <div className="flex justify-center gap-6 text-sm">
          <div>
            <p className="text-muted-foreground text-xs">Khu vực</p>
            <p className="font-bold text-primary">{ticket.zone}</p>
          </div>
          <div>
            <p className="text-muted-foreground text-xs">Hàng</p>
            <p className="font-bold">{ticket.row}</p>
          </div>
          <div>
            <p className="text-muted-foreground text-xs">Ghế</p>
            <p className="font-bold">{ticket.seatNumber}</p>
          </div>
        </div>
      </div>
    </div>
  );
}
