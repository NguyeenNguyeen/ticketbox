"use client";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import { formatCurrency } from "@/lib/utils";

export function RevenueChart({ data }: { data: { day: string; revenue: number }[] }) {
  const chartData = data.length > 0 ? data : [
    { day: "T2", revenue: 0 }, { day: "T3", revenue: 0 }, { day: "T4", revenue: 0 },
    { day: "T5", revenue: 0 }, { day: "T6", revenue: 0 }, { day: "T7", revenue: 0 }, { day: "CN", revenue: 0 }
  ];

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={chartData}>
        <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
        <XAxis dataKey="day" tick={{ fontSize: 12 }} stroke="#94a3b8" />
        <YAxis tick={{ fontSize: 12 }} stroke="#94a3b8" tickFormatter={(v: number) => `${(v / 1000000).toFixed(0)}M`} />
        <Tooltip
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          formatter={((value: any) => [formatCurrency(Number(value || 0)), "Doanh thu"]) as any}
          contentStyle={{ borderRadius: 12, border: "1px solid #e2e8f0", boxShadow: "0 4px 12px rgba(0,0,0,0.05)" }}
        />
        <Line type="monotone" dataKey="revenue" stroke="hsl(250,84%,54%)" strokeWidth={3} dot={{ r: 5, fill: "hsl(250,84%,54%)" }} activeDot={{ r: 7 }} />
      </LineChart>
    </ResponsiveContainer>
  );
}
