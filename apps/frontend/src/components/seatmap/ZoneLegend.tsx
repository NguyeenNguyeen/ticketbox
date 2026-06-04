import { formatCurrency } from "@/lib/utils";
import type { ZoneInfo } from "@/types/seat";

export function ZoneLegend({ zones }: { zones: ZoneInfo[] }) {
  return (
    <div className="flex flex-wrap justify-center gap-4 mt-4 p-4 bg-secondary/50 rounded-xl">
      {zones.map((z) => (
        <div key={z.name} className="flex items-center gap-2 text-sm">
          <span className="w-4 h-4 rounded-full flex-shrink-0" style={{ backgroundColor: z.color }} />
          <span className="font-medium">{z.name}</span>
          <span className="text-muted-foreground">({formatCurrency(z.price)})</span>
          <span className="text-xs text-muted-foreground">- Còn {z.available}/{z.total}</span>
        </div>
      ))}
      <div className="flex items-center gap-2 text-sm">
        <span className="w-4 h-4 rounded-full bg-gray-300 opacity-50 flex-shrink-0" />
        <span className="text-muted-foreground">Đã bán</span>
      </div>
    </div>
  );
}
