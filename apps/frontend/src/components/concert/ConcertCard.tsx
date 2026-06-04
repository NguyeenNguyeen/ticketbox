import Link from "next/link";
import Image from "next/image";
import { Calendar, MapPin } from "lucide-react";
import { Badge } from "@/components/ui/Badge";
import { formatCurrency, formatDate, getStatusLabel, getStatusColor } from "@/lib/utils";
import type { ConcertListItem } from "@/types/concert";

export function ConcertCard({ concert }: { concert: ConcertListItem }) {
  const statusVariant = concert.status === "ON_SALE" ? "success" : concert.status === "UPCOMING" ? "warning" : "secondary";

  return (
    <Link href={`/concerts/${concert.id}`} className="group block">
      <div className="bg-white rounded-2xl border border-border overflow-hidden card-hover">
        {/* Banner */}
        <div className="relative h-48 overflow-hidden">
          <Image src={concert.bannerUrl} alt={concert.title} fill className="object-cover group-hover:scale-105 transition-transform duration-500" />
          <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent" />
          <div className="absolute top-3 right-3">
            <Badge variant={statusVariant}>{getStatusLabel(concert.status)}</Badge>
          </div>
          <div className="absolute bottom-3 left-3">
            <span className="bg-white/90 backdrop-blur-sm text-sm font-bold px-3 py-1 rounded-lg text-primary">
              từ {formatCurrency(concert.priceFrom)}
            </span>
          </div>
        </div>
        {/* Info */}
        <div className="p-4">
          <h3 className="font-bold text-base truncate group-hover:text-primary transition-colors">{concert.title}</h3>
          <div className="mt-2 space-y-1">
            <p className="text-sm text-muted-foreground flex items-center gap-1.5">
              <Calendar className="w-3.5 h-3.5" />{formatDate(concert.date)}
            </p>
            <p className="text-sm text-muted-foreground flex items-center gap-1.5">
              <MapPin className="w-3.5 h-3.5" />{concert.venue}
            </p>
          </div>
          <div className="flex flex-wrap gap-1.5 mt-3">
            {concert.artists.slice(0, 3).map((a) => (
              <span key={a} className="text-xs bg-secondary px-2 py-0.5 rounded-md">{a}</span>
            ))}
            {concert.artists.length > 3 && (
              <span className="text-xs text-muted-foreground">+{concert.artists.length - 3}</span>
            )}
          </div>
        </div>
      </div>
    </Link>
  );
}
