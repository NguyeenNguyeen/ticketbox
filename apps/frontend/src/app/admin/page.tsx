"use client";

import { useEffect, useState } from "react";
import { StatsCards } from "@/components/admin/StatsCards";
import { RevenueChart } from "@/components/admin/RevenueChart";
import { Calendar, TrendingUp } from "lucide-react";
import Link from "next/link";
import { api } from "@/lib/api";
import type { ConcertListItem } from "@/types/concert";

export interface AdminStats {
  totalRevenue: number;
  ticketsSold: number;
  activeEvents: number;
  totalAudience: number;
  revenueChart: { day: string; revenue: number }[];
}

export default function AdminDashboardPage() {
  const [recentConcerts, setRecentConcerts] = useState<ConcertListItem[]>([]);
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadData() {
      try {
        const [concertsData, statsData] = await Promise.all([
          api.get<ConcertListItem[]>("/concerts"),
          api.get<AdminStats>("/admin/stats")
        ]);
        
        if (Array.isArray(concertsData)) {
          setRecentConcerts(concertsData.slice(0, 3));
        }
        if (statsData) {
          setStats(statsData);
        }
      } catch (err) {
        console.error("Failed to load dashboard data", err);
      } finally {
        setLoading(false);
      }
    }
    loadData();
  }, []);

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
      <StatsCards stats={stats} />

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
          <RevenueChart data={stats?.revenueChart || []} />
        </div>

        {/* Recent Concerts */}
        <div className="bg-white rounded-2xl border border-border p-6">
          <h2 className="text-lg font-semibold flex items-center gap-2 mb-4">
            <Calendar className="w-5 h-5 text-primary" />
            Sự kiện gần đây
          </h2>
          <div className="space-y-4">
            {loading ? (
              <div className="text-center py-4 text-muted-foreground text-sm">Đang tải...</div>
            ) : recentConcerts.length === 0 ? (
              <div className="text-center py-4 text-muted-foreground text-sm">Chưa có sự kiện nào</div>
            ) : (
              recentConcerts.map((concert) => (
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
              ))
            )}
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
