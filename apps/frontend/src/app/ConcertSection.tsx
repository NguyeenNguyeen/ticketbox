import { ConcertGrid } from "@/components/concert/ConcertGrid";
import type { ConcertListItem } from "@/types/concert";

interface ConcertSectionProps {
  concerts: ConcertListItem[];
}

export function ConcertSection({ concerts }: ConcertSectionProps) {
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
      <ConcertGrid concerts={concerts} />
    </section>
  );
}
