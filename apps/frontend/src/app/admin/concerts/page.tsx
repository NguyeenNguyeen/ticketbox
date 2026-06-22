"use client";

import { useEffect, useState } from "react";
import { formatCurrency, formatDate, getStatusLabel, getStatusColor } from "@/lib/utils";
import { Plus, Search, Calendar, MapPin, Edit, Trash2 } from "lucide-react";
import Link from "next/link";
import Image from "next/image";
import { api } from "@/lib/api";
import { useToast } from "@/components/ui/Toast";
import type { ConcertListItem } from "@/types/concert";

export default function AdminConcertsPage() {
  const [search, setSearch] = useState("");
  const [allConcerts, setAllConcerts] = useState<ConcertListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();

  const fetchConcerts = async () => {
    try {
      const data = await api.get<ConcertListItem[]>("/concerts");
      setAllConcerts(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Failed to fetch concerts", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchConcerts();
  }, []);

  const concerts = allConcerts.filter((c) =>
    c.title.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div>
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-3xl font-bold">Quản lý sự kiện</h1>
          <p className="text-muted-foreground mt-1">
            Tạo, chỉnh sửa và quản lý các sự kiện concert
          </p>
        </div>
        <Link
          href="/admin/concerts/new"
          className="inline-flex items-center gap-2 bg-primary text-white font-semibold px-5 py-2.5 rounded-xl hover:bg-primary-hover transition-all"
        >
          <Plus className="w-5 h-5" />
          Tạo sự kiện mới
        </Link>
      </div>

      {/* Search */}
      <div className="relative mb-6">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Tìm kiếm sự kiện..."
          className="w-full pl-10 pr-4 py-3 rounded-xl border border-border bg-white focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
        />
      </div>

      {/* Concert List */}
      <div className="space-y-4">
        {concerts.map((concert) => (
          <div
            key={concert.id}
            className="bg-white rounded-2xl border border-border p-4 md:p-6 flex flex-col md:flex-row gap-4 md:gap-6 card-hover"
          >
            {/* Image */}
            <div className="relative w-full md:w-48 h-32 md:h-28 rounded-xl overflow-hidden flex-shrink-0">
              <Image
                src={concert.bannerUrl}
                alt={concert.title}
                fill
                sizes="(max-width: 768px) 100vw, 192px"
                className="object-cover"
              />
            </div>

            {/* Info */}
            <div className="flex-1 min-w-0">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <h3 className="font-bold text-lg truncate">{concert.title}</h3>
                  <div className="flex flex-wrap items-center gap-x-4 gap-y-1 mt-2 text-sm text-muted-foreground">
                    <span className="flex items-center gap-1">
                      <Calendar className="w-4 h-4" />
                      {formatDate(concert.date)}
                    </span>
                    <span className="flex items-center gap-1">
                      <MapPin className="w-4 h-4" />
                      {concert.venue}
                    </span>
                  </div>
                </div>
                <span className={`text-xs font-medium px-3 py-1 rounded-full flex-shrink-0 ${getStatusColor(concert.status)}`}>
                  {getStatusLabel(concert.status)}
                </span>
              </div>

              {/* Ticket summary */}
              <div className="flex flex-wrap gap-2 mt-3">
                <span className="text-xs bg-secondary px-2 py-1 rounded-lg">
                  Giá từ: {formatCurrency(concert.priceFrom || 0)}
                </span>
              </div>

              {/* Actions */}
              <div className="flex items-center gap-3 mt-4">
                <Link
                  href={`/admin/concerts/${concert.id}/edit`}
                  className="inline-flex items-center gap-1 text-sm font-medium text-primary hover:underline"
                >
                  <Edit className="w-4 h-4" />
                  Chỉnh sửa
                </Link>
                <button 
                  onClick={async () => {
                    if (confirm("Bạn có chắc chắn muốn hủy sự kiện này? Hành động này không thể hoàn tác.")) {
                      try {
                        await api.delete(`/admin/concerts/${concert.id}`);
                        toast({ title: "Đã hủy sự kiện thành công", variant: "success" });
                        fetchConcerts();
                      } catch (err: any) {
                        toast({ title: "Lỗi", description: err.message || "Không thể hủy sự kiện", variant: "error" });
                      }
                    }
                  }}
                  className="inline-flex items-center gap-1 text-sm font-medium text-destructive hover:underline"
                >
                  <Trash2 className="w-4 h-4" />
                  Hủy sự kiện
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>

      {concerts.length === 0 && (
        <div className="text-center py-12 text-muted-foreground">
          <Calendar className="w-12 h-12 mx-auto mb-3 opacity-50" />
          <p>Không tìm thấy sự kiện nào</p>
        </div>
      )}
    </div>
  );
}
