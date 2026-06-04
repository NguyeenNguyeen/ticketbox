import Image from "next/image";
import { Calendar, Clock, MapPin, Users } from "lucide-react";
import { Badge } from "@/components/ui/Badge";
import { formatCurrency, formatDate, getStatusLabel } from "@/lib/utils";
import type { Concert } from "@/types/concert";

export function ConcertInfo({ concert }: { concert: Concert }) {
  const statusVariant = concert.status === "ON_SALE" ? "success" : concert.status === "UPCOMING" ? "warning" : "secondary";

  return (
    <div className="bg-white rounded-2xl border border-border overflow-hidden shadow-sm">
      <div className="relative h-64 md:h-80">
        <Image src={concert.bannerUrl} alt={concert.title} fill className="object-cover" priority />
        <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent" />
        <div className="absolute bottom-6 left-6 right-6 text-white">
          <Badge variant={statusVariant} className="mb-2">{getStatusLabel(concert.status)}</Badge>
          <h1 className="text-3xl md:text-4xl font-bold">{concert.title}</h1>
        </div>
      </div>
      <div className="p-6 md:p-8">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          <div className="flex items-center gap-3 p-3 bg-secondary/50 rounded-xl">
            <Calendar className="w-5 h-5 text-primary" />
            <div><p className="text-xs text-muted-foreground">Ngày</p><p className="font-medium text-sm">{formatDate(concert.date)}</p></div>
          </div>
          <div className="flex items-center gap-3 p-3 bg-secondary/50 rounded-xl">
            <Clock className="w-5 h-5 text-primary" />
            <div><p className="text-xs text-muted-foreground">Giờ diễn</p><p className="font-medium text-sm">Mở cửa {concert.doors} • Bắt đầu {concert.showTime}</p></div>
          </div>
          <div className="flex items-center gap-3 p-3 bg-secondary/50 rounded-xl">
            <MapPin className="w-5 h-5 text-primary" />
            <div><p className="text-xs text-muted-foreground">Địa điểm</p><p className="font-medium text-sm">{concert.venue}</p></div>
          </div>
        </div>

        <p className="text-muted-foreground mb-6">{concert.description}</p>

        {/* Artists */}
        <div className="mb-6">
          <h3 className="font-semibold text-sm mb-2 flex items-center gap-2"><Users className="w-4 h-4 text-primary" />Nghệ sĩ</h3>
          <div className="flex flex-wrap gap-2">
            {concert.artists.map((a) => <Badge key={a} variant="outline">{a}</Badge>)}
          </div>
          {concert.artistBio && <p className="text-sm text-muted-foreground mt-2 italic">{concert.artistBio}</p>}
        </div>

        {/* Ticket Categories */}
        <h3 className="font-semibold text-sm mb-3">Bảng giá vé</h3>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border">
                <th className="text-left py-3 px-2 font-semibold">Hạng vé</th>
                <th className="text-right py-3 px-2 font-semibold">Giá</th>
                <th className="text-right py-3 px-2 font-semibold">Còn lại</th>
                <th className="text-right py-3 px-2 font-semibold">Tối đa/người</th>
              </tr>
            </thead>
            <tbody>
              {concert.ticketCategories.map((tc) => (
                <tr key={tc.id} className="border-b border-border last:border-0">
                  <td className="py-3 px-2 flex items-center gap-2">
                    <span className="w-3 h-3 rounded-full" style={{ backgroundColor: tc.color }} />
                    <span className="font-medium">{tc.name}</span>
                  </td>
                  <td className="py-3 px-2 text-right font-semibold text-primary">{formatCurrency(tc.price)}</td>
                  <td className="py-3 px-2 text-right">{tc.availableQuantity}/{tc.totalQuantity}</td>
                  <td className="py-3 px-2 text-right">{tc.maxPerUser} vé</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
