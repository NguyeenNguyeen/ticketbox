"use client";
import { useCartStore } from "@/stores/useCartStore";
import { formatCurrency } from "@/lib/utils";

export function OrderSummary() {
  const items = useCartStore((s) => s.items);
  const getTotalAmount = useCartStore((s) => s.getTotalAmount);

  const grouped = items.reduce((acc, item) => {
    if (!acc[item.zone]) acc[item.zone] = [];
    acc[item.zone].push(item);
    return acc;
  }, {} as Record<string, typeof items>);

  return (
    <div className="bg-white rounded-2xl border border-border p-6">
      <h3 className="font-semibold mb-4">Chi tiết đơn hàng</h3>
      <div className="space-y-3">
        {Object.entries(grouped).map(([zone, seats]) => (
          <div key={zone}>
            <p className="text-xs font-semibold text-muted-foreground uppercase mb-1">{zone}</p>
            {seats.map((s) => (
              <div key={s.seatId} className="flex justify-between text-sm py-1">
                <span>Hàng {s.row} - Ghế {s.number}</span>
                <span className="font-medium">{formatCurrency(s.price)}</span>
              </div>
            ))}
          </div>
        ))}
      </div>
      <hr className="my-4 border-border" />
      <div className="flex justify-between items-center">
        <span className="font-semibold">Tổng cộng</span>
        <span className="text-xl font-bold text-primary">{formatCurrency(getTotalAmount())}</span>
      </div>
    </div>
  );
}
