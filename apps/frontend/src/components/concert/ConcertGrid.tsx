"use client";
import { useState } from "react";
import { Search, Music } from "lucide-react";
import { ConcertCard } from "./ConcertCard";
import { cn } from "@/lib/utils";
import type { ConcertListItem } from "@/types/concert";

const tabs = [
  { label: "Tất cả", value: "ALL" },
  { label: "Đang bán", value: "ON_SALE" },
  { label: "Sắp mở bán", value: "UPCOMING" },
  { label: "Đã diễn ra", value: "COMPLETED" },
];

export function ConcertGrid({ concerts }: { concerts: ConcertListItem[] }) {
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("ALL");

  const filtered = concerts.filter((c) => {
    const matchSearch = c.title.toLowerCase().includes(search.toLowerCase()) || c.artists.some((a) => a.toLowerCase().includes(search.toLowerCase()));
    const matchFilter = filter === "ALL" || c.status === filter;
    return matchSearch && matchFilter;
  });

  return (
    <div>
      {/* Search & Filters */}
      <div className="flex flex-col sm:flex-row gap-4 mb-8">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
          <input
            type="text" value={search} onChange={(e) => setSearch(e.target.value)}
            placeholder="Tìm theo tên sự kiện hoặc nghệ sĩ..."
            className="w-full pl-10 pr-4 py-3 rounded-xl border border-border bg-white focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
          />
        </div>
        <div className="flex gap-2">
          {tabs.map((t) => (
            <button key={t.value} onClick={() => setFilter(t.value)}
              className={cn("px-4 py-2 rounded-xl text-sm font-medium transition-all whitespace-nowrap",
                filter === t.value ? "bg-primary text-white" : "bg-secondary text-muted-foreground hover:text-foreground"
              )}>
              {t.label}
            </button>
          ))}
        </div>
      </div>

      {/* Grid */}
      {filtered.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filtered.map((c) => <ConcertCard key={c.id} concert={c} />)}
        </div>
      ) : (
        <div className="text-center py-16">
          <Music className="w-12 h-12 text-muted-foreground mx-auto mb-3 opacity-50" />
          <p className="text-muted-foreground">Không tìm thấy sự kiện nào</p>
        </div>
      )}
    </div>
  );
}
