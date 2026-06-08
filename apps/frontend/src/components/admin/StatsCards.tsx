"use client";
import { DollarSign, Ticket, Calendar, Users } from "lucide-react";
import { formatCurrency } from "@/lib/utils";
import type { AdminStats } from "@/app/admin/page";

export function StatsCards({ stats }: { stats: AdminStats | null }) {
  const cards = [
    { label: "Tổng doanh thu", value: stats?.totalRevenue || 0, icon: DollarSign, trend: "+12.5%", color: "from-purple-500/10 to-purple-500/5", iconColor: "text-purple-600 bg-purple-100" },
    { label: "Vé đã bán", value: stats?.ticketsSold || 0, icon: Ticket, trend: "+8.2%", color: "from-blue-500/10 to-blue-500/5", iconColor: "text-blue-600 bg-blue-100" },
    { label: "Sự kiện", value: stats?.activeEvents || 0, icon: Calendar, trend: "ON_SALE", color: "from-green-500/10 to-green-500/5", iconColor: "text-green-600 bg-green-100" },
    { label: "Tổng khán giả", value: stats?.totalAudience || 0, icon: Users, trend: "Khách hàng", color: "from-amber-500/10 to-amber-500/5", iconColor: "text-amber-600 bg-amber-100" },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map((s) => (
        <div key={s.label} className={`bg-gradient-to-br ${s.color} bg-white rounded-2xl border border-border p-5`}>
          <div className="flex items-center justify-between mb-3">
            <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${s.iconColor}`}>
              <s.icon className="w-5 h-5" />
            </div>
            <span className="text-xs font-medium text-green-600 bg-green-50 px-2 py-0.5 rounded-full">{s.trend}</span>
          </div>
          <p className="text-2xl font-bold">{typeof s.value === "number" && s.value > 1000 ? formatCurrency(s.value) : s.value.toLocaleString("vi-VN")}</p>
          <p className="text-sm text-muted-foreground mt-1">{s.label}</p>
        </div>
      ))}
    </div>
  );
}
