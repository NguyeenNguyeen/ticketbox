"use client";

import { StatsCards } from "@/components/admin/StatsCards";
import { RevenueChart } from "@/components/admin/RevenueChart";
import { mockConcerts } from "@/mocks/concerts";
import { Calendar, TrendingUp } from "lucide-react";
import Link from "next/link";

export default function AdminDashboardPage() {
  const recentConcerts = mockConcerts.slice(0, 3);

  return (
    <div>
      {/* Page Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold">Dashboard</h1>
        <p className="text-muted-foreground mt-1">
          Tổng quan hoạt động và doanh thu sự kiện
        </p>
      </div>

      {/* Stats Cards */}
      <StatsCards />

      {/* Charts & Recent */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-8">
        {/* Revenue Chart */}
        <div className="lg:col-span-2 bg-white rounded-2xl border border-border p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-lg font-semibold flex items-center gap-2">
                <TrendingUp className="w-5 h-5 text-primary" />
                Doanh thu 7 ngày gần nhất
              </h2>
            </div>
          </div>
          <RevenueChart />
        </div>

        {/* Recent Concerts */}
        <div className="bg-white rounded-2xl border border-border p-6">
          <h2 className="text-lg font-semibold flex items-center gap-2 mb-4">
            <Calendar className="w-5 h-5 text-primary" />
            Sự kiện gần đây
          </h2>
          <div className="space-y-4">
            {recentConcerts.map((concert) => (
              <Link
                key={concert.id}
                href={`/admin/concerts/${concert.id}/edit`}
                className="block p-3 rounded-xl hover:bg-secondary transition-colors"
              >
                <p className="font-medium text-sm truncate">{concert.title}</p>
                <p className="text-xs text-muted-foreground mt-1">
                  {concert.venue}
                </p>
                <div className="flex items-center justify-between mt-2">
                  <span
                    className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                      concert.status === "ON_SALE"
                        ? "bg-success/10 text-success"
                        : concert.status === "UPCOMING"
                        ? "bg-blue-50 text-blue-600"
                        : "bg-muted text-muted-foreground"
                    }`}
                  >
                    {concert.status === "ON_SALE"
                      ? "Đang bán"
                      : concert.status === "UPCOMING"
                      ? "Sắp mở bán"
                      : concert.status}
                  </span>
                </div>
              </Link>
            ))}
          </div>
          <Link
            href="/admin/concerts"
            className="block text-center text-sm text-primary font-medium mt-4 hover:underline"
          >
            Xem tất cả →
          </Link>
        </div>
      </div>
    </div>
  );
}
