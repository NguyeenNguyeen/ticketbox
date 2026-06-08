"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { formatCurrency } from "@/lib/utils";
import { Minus, Plus } from "lucide-react";

interface TicketCategory {
  id: number;
  name: string;
  price: number;
  totalQuantity: number;
  availableQuantity: number;
  maxPerUser: number;
  color?: string;
  saleStartTime?: string;
}

interface SelectedItem {
  categoryId: number;
  name: string;
  quantity: number;
  price: number;
}

interface TicketSelectorProps {
  concertId: string;
  isCancelled?: boolean;
  onSelectionChange: (selectedItems: SelectedItem[]) => void;
}

export function TicketSelector({ concertId, isCancelled, onSelectionChange }: TicketSelectorProps) {
  const [categories, setCategories] = useState<TicketCategory[]>([]);
  const [loading, setLoading] = useState(true);
  const [quantities, setQuantities] = useState<Record<number, number>>({});

  useEffect(() => {
    async function fetchCategories() {
      try {
        const data = await api.get<TicketCategory[]>(`/concerts/${concertId}/categories`);
        setCategories(data || []);
      } catch (err) {
        console.error("Failed to fetch categories", err);
      } finally {
        setLoading(false);
      }
    }
    fetchCategories();
  }, [concertId]);

  const updateQuantity = (cat: TicketCategory, delta: number) => {
    const current = quantities[cat.id] || 0;
    // Cap at the lesser of maxPerUser (from API) and availableQuantity
    const max = Math.min(cat.maxPerUser, cat.availableQuantity);
    const next = Math.max(0, Math.min(max, current + delta));
    const newQuantities = { ...quantities, [cat.id]: next };

    setQuantities(newQuantities);

    const selectedItems = categories
      .filter((c) => newQuantities[c.id] > 0)
      .map((c) => ({
        categoryId: c.id,
        name: c.name,
        price: c.price,
        quantity: newQuantities[c.id],
      }));
    onSelectionChange(selectedItems);
  };

  if (loading) {
    return <div className="text-center py-12 text-muted-foreground">Đang tải danh sách vé...</div>;
  }

  if (categories.length === 0) {
    return <div className="text-center py-12 text-muted-foreground">Sự kiện này chưa có thông tin vé.</div>;
  }

  return (
    <div className="space-y-4">
      {isCancelled && (
        <div className="bg-destructive/10 text-destructive p-4 rounded-xl text-center mb-6 border border-destructive/20 font-medium">
          Sự kiện này đã bị hoãn hoặc huỷ. Bạn không thể đặt vé vào lúc này.
        </div>
      )}
      {categories.map((cat) => {
        const isSoldOut = cat.availableQuantity === 0;
        const disabled = isSoldOut || isCancelled;
        const currentQty = quantities[cat.id] || 0;
        const maxAllowed = Math.min(cat.maxPerUser, cat.availableQuantity);

        return (
          <div
            key={cat.id}
            className={`flex flex-col sm:flex-row sm:items-center justify-between p-5 rounded-2xl border ${
              disabled
                ? "bg-secondary/50 border-border opacity-70"
                : "bg-white border-border hover:border-primary/30 transition-colors"
            }`}
          >
            <div className="flex-1 mb-4 sm:mb-0">
              <div className="flex items-center gap-2 mb-1">
                {cat.color && (
                  <span
                    className="w-3 h-3 rounded-full flex-shrink-0"
                    style={{ backgroundColor: cat.color }}
                  />
                )}
                <h3 className="font-bold text-lg">{cat.name}</h3>
                {isSoldOut && (
                  <span className="text-xs font-semibold bg-destructive/10 text-destructive px-2 py-1 rounded-full">
                    Hết vé
                  </span>
                )}
              </div>
              <p className="text-primary font-bold text-xl">{formatCurrency(cat.price)}</p>
              <p className="text-sm text-muted-foreground mt-1">
                Còn lại: {cat.availableQuantity} vé
                {cat.maxPerUser && (
                  <span className="ml-2 text-xs">(tối đa {cat.maxPerUser} vé/tài khoản)</span>
                )}
              </p>
            </div>

            <div className="flex items-center gap-4 bg-secondary/50 rounded-xl p-1">
              <button
                onClick={() => updateQuantity(cat, -1)}
                disabled={currentQty === 0 || disabled}
                className="w-10 h-10 flex items-center justify-center rounded-lg bg-white shadow-sm border border-border disabled:opacity-50 disabled:cursor-not-allowed hover:bg-secondary transition-colors"
              >
                <Minus className="w-4 h-4" />
              </button>
              <span className="w-8 text-center font-bold text-lg">{currentQty}</span>
              <button
                onClick={() => updateQuantity(cat, 1)}
                disabled={currentQty >= maxAllowed || disabled}
                className="w-10 h-10 flex items-center justify-center rounded-lg bg-white shadow-sm border border-border disabled:opacity-50 disabled:cursor-not-allowed hover:bg-secondary transition-colors"
              >
                <Plus className="w-4 h-4" />
              </button>
            </div>
          </div>
        );
      })}
    </div>
  );
}
