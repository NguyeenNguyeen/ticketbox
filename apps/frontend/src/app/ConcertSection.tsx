"use client";

import { ConcertGrid } from "@/components/concert/ConcertGrid";
import { mockConcerts } from "@/mocks/concerts";
import type { ConcertListItem } from "@/types/concert";

export function ConcertSection() {
  const concertList: ConcertListItem[] = mockConcerts.map((c) => ({
    id: c.id,
    title: c.title,
    venue: c.venue,
    date: c.date,
    bannerUrl: c.bannerUrl,
    status: c.status,
    priceFrom: Math.min(...c.ticketCategories.map((tc) => tc.price)),
    artists: c.artists,
  }));

  return (
    <section id="concerts" className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
      <div className="text-center mb-12">
        <h2 className="text-3xl md:text-4xl font-bold text-foreground mb-4">
          Sự kiện <span className="text-gradient-primary">nổi bật</span>
        </h2>
        <p className="text-muted-foreground text-lg max-w-2xl mx-auto">
          Khám phá các concert âm nhạc hot nhất đang chờ bạn. Đặt vé ngay để không bỏ lỡ!
        </p>
      </div>
      <ConcertGrid concerts={concertList} />
    </section>
  );
}
