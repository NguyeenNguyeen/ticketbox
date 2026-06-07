"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { formatCurrency } from "@/lib/utils";

interface OrderHistoryDto {
  id: string;
  status: string;
  totalAmount: number;
  createdAt: string;
  username: string;
}

export default function OrdersPage() {
  const [orders, setOrders] = useState<OrderHistoryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadOrders() {
      try {
        const data = await api.get<OrderHistoryDto[]>("/orders/history");
        setOrders(data || []);
      } catch (e) {
        console.error(e);
        setError("Không thể tải lịch sử mua hàng.");
      } finally {
        setLoading(false);
      }
    }
    loadOrders();
  }, []);

  return (
    <div className="min-h-screen flex flex-col">
      <Header />
      <main className="flex-1 max-w-4xl w-full mx-auto p-6 mt-8">
        <h1 className="text-3xl font-bold mb-8">Lịch sử mua hàng</h1>
        {loading ? (
          <div>Đang tải...</div>
        ) : error ? (
          <div className="text-destructive">{error}</div>
        ) : orders.length === 0 ? (
          <div className="text-muted-foreground">Bạn chưa có đơn hàng nào.</div>
        ) : (
          <div className="space-y-4">
            {orders.map((order) => (
              <div key={order.id} className="border border-border rounded-xl p-5 bg-white shadow-sm flex items-center justify-between hover:shadow-md transition-shadow">
                <div>
                  <p className="font-semibold text-lg">Mã đơn: #{order.id}</p>
                  <p className="text-sm text-muted-foreground mt-1">Ngày đặt: {new Date(order.createdAt).toLocaleString()}</p>
                </div>
                <div className="text-right">
                  <p className="font-bold text-primary text-xl">{formatCurrency(order.totalAmount)}</p>
                  <span className={`inline-block mt-2 px-3 py-1 rounded-full text-xs font-medium ${
                    order.status === "PAID" ? "bg-success/10 text-success" : 
                    order.status === "CANCELLED" ? "bg-destructive/10 text-destructive" : 
                    "bg-warning/10 text-warning"
                  }`}>
                    {order.status}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
      <Footer />
    </div>
  );
}
