"use client";
import { CreditCard, Smartphone, Check } from "lucide-react";
import { cn } from "@/lib/utils";
import type { PaymentMethod } from "@/types/order";

interface Props { selected: PaymentMethod | null; onSelect: (m: PaymentMethod) => void; }

const methods = [
  { id: "VNPAY" as PaymentMethod, name: "VNPAY", desc: "Thanh toán qua thẻ ngân hàng", icon: CreditCard, color: "border-blue-500 bg-blue-50" },
  { id: "MOMO" as PaymentMethod, name: "MoMo", desc: "Thanh toán qua ví MoMo", icon: Smartphone, color: "border-pink-500 bg-pink-50" },
];

export function PaymentMethodSelector({ selected, onSelect }: Props) {
  return (
    <div className="space-y-3">
      <h3 className="font-semibold">Phương thức thanh toán</h3>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {methods.map((m) => (
          <button key={m.id} onClick={() => onSelect(m.id)}
            className={cn("relative flex items-center gap-3 p-4 rounded-xl border-2 transition-all text-left",
              selected === m.id ? "border-primary bg-primary/5" : "border-border hover:border-primary/30"
            )}>
            {selected === m.id && (
              <div className="absolute top-2 right-2 w-5 h-5 rounded-full bg-primary flex items-center justify-center">
                <Check className="w-3 h-3 text-white" />
              </div>
            )}
            <div className={cn("w-10 h-10 rounded-lg flex items-center justify-center", m.color)}>
              <m.icon className="w-5 h-5" />
            </div>
            <div>
              <p className="font-semibold text-sm">{m.name}</p>
              <p className="text-xs text-muted-foreground">{m.desc}</p>
            </div>
          </button>
        ))}
      </div>
    </div>
  );
}
