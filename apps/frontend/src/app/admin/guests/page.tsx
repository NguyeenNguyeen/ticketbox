"use client";

import { Users, Search, Download } from "lucide-react";
import { useState } from "react";

interface GuestEntry {
  id: string;
  name: string;
  email: string;
  phone: string;
  concert: string;
  sponsor: string;
  status: "confirmed" | "pending" | "checked_in";
  importedAt: string;
}

const mockGuests: GuestEntry[] = [
  {
    id: "g1",
    name: "Trần Thanh Hải",
    email: "hai.tran@sponsor.com",
    phone: "0901234567",
    concert: "Anh Trai Say Hi",
    sponsor: "Samsung Vietnam",
    status: "confirmed",
    importedAt: "2026-12-15T10:00:00+07:00",
  },
  {
    id: "g2",
    name: "Nguyễn Minh Châu",
    email: "chau.nguyen@brand.com",
    phone: "0912345678",
    concert: "Anh Trai Say Hi",
    sponsor: "Vinamilk",
    status: "pending",
    importedAt: "2026-12-15T10:00:00+07:00",
  },
  {
    id: "g3",
    name: "Lê Phương Anh",
    email: "anh.le@company.com",
    phone: "0923456789",
    concert: "Chị Đẹp Đạp Gió Rẽ Sóng",
    sponsor: "L'Oréal Vietnam",
    status: "confirmed",
    importedAt: "2027-03-01T08:00:00+07:00",
  },
  {
    id: "g4",
    name: "Phạm Đức Minh",
    email: "minh.pham@vip.com",
    phone: "0934567890",
    concert: "Em Xinh Say Hi",
    sponsor: "Shopee Vietnam",
    status: "checked_in",
    importedAt: "2027-02-10T14:00:00+07:00",
  },
  {
    id: "g5",
    name: "Hoàng Thu Trang",
    email: "trang.hoang@media.com",
    phone: "0945678901",
    concert: "Anh Trai Vượt Ngàn Chông Gai",
    sponsor: "TikTok Vietnam",
    status: "pending",
    importedAt: "2027-01-08T09:00:00+07:00",
  },
];

export default function AdminGuestsPage() {
  const [search, setSearch] = useState("");
  const [filterConcert, setFilterConcert] = useState("all");

  const concerts = [...new Set(mockGuests.map((g) => g.concert))];

  const filtered = mockGuests.filter((g) => {
    const matchSearch =
      g.name.toLowerCase().includes(search.toLowerCase()) ||
      g.email.toLowerCase().includes(search.toLowerCase()) ||
      g.sponsor.toLowerCase().includes(search.toLowerCase());
    const matchConcert = filterConcert === "all" || g.concert === filterConcert;
    return matchSearch && matchConcert;
  });

  const statusMap = {
    confirmed: { label: "Đã xác nhận", color: "bg-success/10 text-success" },
    pending: { label: "Chờ xác nhận", color: "bg-warning/10 text-warning" },
    checked_in: { label: "Đã check-in", color: "bg-blue-50 text-blue-600" },
  };

  return (
    <div>
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-3xl font-bold flex items-center gap-2">
            <Users className="w-8 h-8 text-primary" />
            Khách mời VIP
          </h1>
          <p className="text-muted-foreground mt-1">
            Danh sách khách mời được import từ CSV nhãn hàng tài trợ
          </p>
        </div>
        <button className="inline-flex items-center gap-2 bg-white text-foreground font-medium px-5 py-2.5 rounded-xl border border-border hover:bg-secondary transition-all">
          <Download className="w-5 h-5" />
          Xuất CSV
        </button>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-3 mb-6">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Tìm theo tên, email, nhãn hàng..."
            className="w-full pl-10 pr-4 py-3 rounded-xl border border-border bg-white focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
          />
        </div>
        <select
          value={filterConcert}
          onChange={(e) => setFilterConcert(e.target.value)}
          className="px-4 py-3 rounded-xl border border-border bg-white focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
        >
          <option value="all">Tất cả sự kiện</option>
          {concerts.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
      </div>

      {/* Table */}
      <div className="bg-white rounded-2xl border border-border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border bg-secondary/50">
                <th className="text-left px-6 py-4 text-sm font-semibold">Họ tên</th>
                <th className="text-left px-6 py-4 text-sm font-semibold">Email</th>
                <th className="text-left px-6 py-4 text-sm font-semibold">Sự kiện</th>
                <th className="text-left px-6 py-4 text-sm font-semibold">Nhãn hàng</th>
                <th className="text-left px-6 py-4 text-sm font-semibold">Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((guest) => (
                <tr
                  key={guest.id}
                  className="border-b border-border last:border-0 hover:bg-secondary/30 transition-colors"
                >
                  <td className="px-6 py-4">
                    <div className="font-medium">{guest.name}</div>
                    <div className="text-sm text-muted-foreground">{guest.phone}</div>
                  </td>
                  <td className="px-6 py-4 text-sm">{guest.email}</td>
                  <td className="px-6 py-4 text-sm">{guest.concert}</td>
                  <td className="px-6 py-4 text-sm">{guest.sponsor}</td>
                  <td className="px-6 py-4">
                    <span
                      className={`text-xs font-medium px-3 py-1 rounded-full ${statusMap[guest.status].color}`}
                    >
                      {statusMap[guest.status].label}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {filtered.length === 0 && (
          <div className="text-center py-12 text-muted-foreground">
            <Users className="w-12 h-12 mx-auto mb-3 opacity-50" />
            <p>Không tìm thấy khách mời nào</p>
          </div>
        )}
      </div>

      <div className="text-sm text-muted-foreground mt-4">
        Hiển thị {filtered.length} / {mockGuests.length} khách mời
      </div>
    </div>
  );
}
