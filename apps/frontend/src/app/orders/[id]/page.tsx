"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { api } from "@/lib/api";
import { formatCurrency } from "@/lib/utils";
import { ETicket } from "@/components/ticket/ETicket";
import type { ETicket as ETicketType } from "@/types/order";
import { Loader2, ArrowLeft, Ticket } from "lucide-react";
import Link from "next/link";

interface OrderHistoryDto {
  id: string;
  status: string;
  totalAmount: number;
  createdAt: string;
  username: string;
}

export default function OrderDetailsPage() {
  const { id } = useParams();
  const router = useRouter();
  const [order, setOrder] = useState<OrderHistoryDto | null>(null);
  const [tickets, setTickets] = useState<ETicketType[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadData() {
      if (!id || typeof id !== "string") return;
      try {
        setLoading(true);
        // Fetch order details by finding from history
        const orders = await api.get<OrderHistoryDto[]>("/orders/history");
        const matchedOrder = orders?.find(o => o.id === id);
        
        if (!matchedOrder) {
          setError("Không tìm thấy đơn hàng.");
          setLoading(false);
          return;
        }
        
        setOrder(matchedOrder);

        // Fetch tickets for this order
        const fetchedTickets = await api.get<ETicketType[]>(`/tickets/order/${id}`);
        setTickets(fetchedTickets || []);
      } catch (err) {
        console.error("Failed to load order details:", err);
        setError("Không thể tải chi tiết đơn hàng.");
      } finally {
        setLoading(false);
      }
    }
    
    loadData();
  }, [id]);

  return (
    <div className="min-h-screen flex flex-col">
      <Header />
      <main className="flex-1 max-w-5xl w-full mx-auto p-6 mt-8">
        <div className="mb-6">
          <Link href="/orders" className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground transition-colors">
            <ArrowLeft className="w-4 h-4 mr-2" /> Quay lại danh sách đơn hàng
          </Link>
        </div>

        {loading ? (
          <div className="flex items-center gap-3 py-10">
            <Loader2 className="w-6 h-6 animate-spin text-primary" />
            <span className="text-muted-foreground">Đang tải chi tiết đơn hàng...</span>
          </div>
        ) : error ? (
          <div className="bg-destructive/10 text-destructive p-4 rounded-xl">{error}</div>
        ) : order ? (
          <div className="space-y-8 animate-fade-in">
            <div className="bg-white border border-border shadow-sm rounded-2xl p-6 md:p-8">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-border pb-6 mb-8">
                <div>
                  <h1 className="text-2xl font-bold">Chi tiết đơn hàng #{order.id}</h1>
                  <p className="text-muted-foreground mt-1">Ngày đặt: {new Date(order.createdAt).toLocaleString()}</p>
                </div>
                <div className="text-left md:text-right">
                  <span className={`inline-block px-4 py-1.5 rounded-full text-sm font-semibold mb-2 ${
                    order.status === "PAID" ? "bg-success/10 text-success" : 
                    order.status === "CANCELLED" ? "bg-destructive/10 text-destructive" : 
                    "bg-warning/10 text-warning"
                  }`}>
                    Trạng thái: {order.status}
                  </span>
                  <p className="font-bold text-2xl text-primary">{formatCurrency(order.totalAmount)}</p>
                </div>
              </div>

              <div>
                <div className="flex items-center gap-2 mb-6">
                  <Ticket className="w-6 h-6 text-primary" />
                  <h2 className="text-xl font-bold">Vé điện tử của bạn</h2>
                </div>
                
                {tickets.length > 0 ? (
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {tickets.map(ticket => (
                      <div key={ticket.id} className="transition-transform hover:-translate-y-1 duration-300">
                        <ETicket ticket={ticket} />
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="bg-secondary/30 rounded-xl p-10 text-center border border-dashed border-border">
                    <p className="text-muted-foreground font-medium">Không có vé nào được tìm thấy cho đơn hàng này.</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        ) : null}
      </main>
      <Footer />
    </div>
  );
}
