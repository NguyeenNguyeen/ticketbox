"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { formatCurrency } from "@/lib/utils";

interface OrderHistoryDto {
  id: string;
  status: string;
  totalAmount: number;
  createdAt: string;
  username: string;
}

export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<OrderHistoryDto[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadOrders() {
      try {
        const data = await api.get<OrderHistoryDto[]>("/admin/orders");
        setOrders(data || []);
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    }
    loadOrders();
  }, []);

  return (
    <div>
      <h1 className="text-3xl font-bold mb-8">Quản lý Đơn hàng</h1>
      {loading ? (
        <div>Đang tải...</div>
      ) : (
        <div className="bg-white rounded-2xl border border-border overflow-hidden">
          <table className="w-full text-sm text-left">
            <thead className="bg-secondary text-muted-foreground">
              <tr>
                <th className="px-6 py-4 font-medium">Mã Đơn</th>
                <th className="px-6 py-4 font-medium">Khách Hàng</th>
                <th className="px-6 py-4 font-medium">Tổng Tiền</th>
                <th className="px-6 py-4 font-medium">Trạng Thái</th>
                <th className="px-6 py-4 font-medium">Ngày Đặt</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {orders.map((order) => (
                <tr key={order.id} className="hover:bg-secondary/50 transition-colors">
                  <td className="px-6 py-4 font-medium">#{order.id}</td>
                  <td className="px-6 py-4">{order.username}</td>
                  <td className="px-6 py-4 font-bold text-primary">{formatCurrency(order.totalAmount)}</td>
                  <td className="px-6 py-4">
                    <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                      order.status === "PAID" ? "bg-success/10 text-success" : 
                      order.status === "CANCELLED" ? "bg-destructive/10 text-destructive" : 
                      "bg-warning/10 text-warning"
                    }`}>
                      {order.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-muted-foreground">
                    {new Date(order.createdAt).toLocaleString()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
