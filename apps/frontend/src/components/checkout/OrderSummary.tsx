"use client";
import { useCartStore } from "@/stores/useCartStore";
import { formatCurrency } from "@/lib/utils";
import { Ticket } from "lucide-react";

export function OrderSummary() {
  const items = useCartStore((s) => s.items);
  const concertTitle = useCartStore((s) => s.concertTitle);
  const totalAmount = useCartStore((s) => s.getTotalAmount());
  const totalTickets = items.reduce((sum, item) => sum + item.quantity, 0);

  return (
    <div className="bg-white rounded-2xl border border-border p-6 sticky top-24">
      <h2 className="text-xl font-bold mb-6">Tóm tắt đơn hàng</h2>

      <div className="mb-6">
        <h3 className="font-semibold text-foreground mb-1">{concertTitle}</h3>
        <p className="text-sm text-muted-foreground">{totalTickets} vé</p>
      </div>

      <div className="space-y-4 mb-6 max-h-[300px] overflow-y-auto pr-2 custom-scrollbar">
        {items.map((item) => (
          <div key={item.categoryId} className="flex items-start justify-between gap-4 p-3 bg-secondary/50 rounded-xl">
            <div className="flex items-start gap-3">
              <div className="mt-1 bg-white p-1.5 rounded-lg shadow-sm border border-border">
                <Ticket className="w-4 h-4 text-primary" />
              </div>
              <div>
                <p className="font-bold text-sm">Hạng: {item.name}</p>
                <p className="text-xs text-muted-foreground mt-0.5">Số lượng: {item.quantity}</p>
              </div>
            </div>
            <p className="font-semibold text-sm">{formatCurrency(item.price * item.quantity)}</p>
          </div>
        ))}
      </div>

      <div className="border-t border-border pt-4 space-y-3">
        <div className="flex items-center justify-between text-sm">
          <span className="text-muted-foreground">Tạm tính</span>
          <span className="font-medium">{formatCurrency(totalAmount)}</span>
        </div>
        <div className="flex items-center justify-between text-sm">
          <span className="text-muted-foreground">Phí xử lý</span>
          <span className="font-medium">Miễn phí</span>
        </div>
        <div className="flex items-center justify-between pt-3 border-t border-dashed border-border mt-3">
          <span className="font-bold">Tổng cộng</span>
          <span className="text-2xl font-bold text-primary">{formatCurrency(totalAmount)}</span>
        </div>
      </div>
    </div>
  );
}
