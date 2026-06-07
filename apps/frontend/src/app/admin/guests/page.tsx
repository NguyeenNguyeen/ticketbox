"use client";

import { Users, Search, Download, UploadCloud, X, CheckCircle2, AlertTriangle } from "lucide-react";
import { useState, useRef } from "react";

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
  const [isImporting, setIsImporting] = useState(false);
  const [csvFile, setCsvFile] = useState<File | null>(null);
  const [previewData, setPreviewData] = useState<{row: number, data: string[], error: string}[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const concerts = [...new Set(mockGuests.map((g) => g.concert))];

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setCsvFile(file);
      
      // Simple manual CSV parsing for demo
      const reader = new FileReader();
      reader.onload = (event) => {
        const text = event.target?.result as string;
        if (text) {
          const lines = text.split('\n').filter(line => line.trim().length > 0);
          const preview = lines.slice(1, 6).map((line, index) => {
            const cols = line.split(',');
            let error = "";
            // Mock validation: check if email is missing or invalid
            if (cols.length < 2 || !cols[1]?.includes('@')) {
              error = "Email không hợp lệ hoặc bị thiếu";
            } else if (cols.length < 3 || cols[2]?.length < 9) {
              error = "Số điện thoại không hợp lệ";
            }
            return { row: index + 2, data: cols, error };
          });
          setPreviewData(preview);
          setIsImporting(true);
        }
      };
      reader.readAsText(file);
    }
  };

  const cancelImport = () => {
    setIsImporting(false);
    setCsvFile(null);
    setPreviewData([]);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const confirmImport = () => {
    alert("Đã gửi yêu cầu import danh sách khách mời!");
    cancelImport();
  };

  const filtered = mockGuests.filter((g) => {
    const matchSearch =
      g.name.toLowerCase().includes(search.toLowerCase()) ||
      g.email.toLowerCase().includes(search.toLowerCase()) ||
      g.sponsor.toLowerCase().includes(search.toLowerCase());
    const matchConcert = filterConcert === "all" || g.concert === filterConcert;
    return matchSearch && matchConcert;
  });

  const statusMap: Record<string, { label: string; color: string }> = {
    confirmed: { label: "Đã xác nhận", color: "bg-success/10 text-success" },
    pending: { label: "Chờ xác nhận", color: "bg-warning/10 text-warning" },
    checked_in: { label: "Đã check-in", color: "bg-blue-50 text-blue-600" },
  };

  const handleExportCsv = () => {
    if (filtered.length === 0) {
      alert("Không có dữ liệu để xuất");
      return;
    }

    // Create CSV header
    const headers = ["Họ tên", "Email", "SĐT", "Sự kiện", "Nhãn hàng", "Trạng thái"];
    const rows = filtered.map(g => [
      `"${g.name}"`, 
      `"${g.email}"`, 
      `"${g.phone}"`, 
      `"${g.concert}"`, 
      `"${g.sponsor}"`, 
      `"${statusMap[g.status]?.label || g.status}"`
    ]);

    const csvContent = [headers.join(","), ...rows.map(r => r.join(","))].join("\n");
    const blob = new Blob(["\uFEFF" + csvContent], { type: "text/csv;charset=utf-8;" });
    const link = document.createElement("a");
    const url = URL.createObjectURL(blob);
    link.setAttribute("href", url);
    link.setAttribute("download", `khach_moi_${new Date().getTime()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
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
        <div className="flex gap-3">
          <button 
            onClick={() => fileInputRef.current?.click()}
            className="inline-flex items-center gap-2 bg-primary text-white font-medium px-5 py-2.5 rounded-xl hover:bg-primary-hover transition-all shadow-sm"
          >
            <UploadCloud className="w-5 h-5" />
            Import CSV
          </button>
          <input 
            type="file" 
            accept=".csv" 
            ref={fileInputRef} 
            onChange={handleFileUpload} 
            className="hidden" 
          />
          <button 
            onClick={handleExportCsv}
            className="inline-flex items-center gap-2 bg-white text-foreground font-medium px-5 py-2.5 rounded-xl border border-border hover:bg-secondary transition-all"
          >
            <Download className="w-5 h-5" />
            Xuất CSV
          </button>
        </div>
      </div>

      {/* Import Preview Modal / Section */}
      {isImporting && (
        <div className="bg-white rounded-2xl border border-border overflow-hidden mb-8 shadow-sm animate-fade-in relative">
          <button onClick={cancelImport} className="absolute top-4 right-4 text-muted-foreground hover:text-foreground">
            <X className="w-5 h-5" />
          </button>
          <div className="p-6 border-b border-border bg-secondary/30">
            <h2 className="text-lg font-bold">Xem trước dữ liệu Import</h2>
            <p className="text-sm text-muted-foreground">Tệp: {csvFile?.name}</p>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-secondary/50 border-b border-border">
                  <th className="text-left px-6 py-3 font-semibold">Dòng</th>
                  <th className="text-left px-6 py-3 font-semibold">Dữ liệu</th>
                  <th className="text-left px-6 py-3 font-semibold">Kiểm tra</th>
                </tr>
              </thead>
              <tbody>
                {previewData.map((row, i) => (
                  <tr key={i} className={`border-b border-border ${row.error ? 'bg-destructive/5' : ''}`}>
                    <td className="px-6 py-3 text-muted-foreground">#{row.row}</td>
                    <td className="px-6 py-3 font-mono text-xs">{row.data.join(', ')}</td>
                    <td className="px-6 py-3">
                      {row.error ? (
                        <span className="flex items-center gap-1 text-destructive font-medium">
                          <AlertTriangle className="w-4 h-4" /> {row.error}
                        </span>
                      ) : (
                        <span className="flex items-center gap-1 text-success font-medium">
                          <CheckCircle2 className="w-4 h-4" /> Hợp lệ
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="p-6 bg-secondary/30 flex justify-end gap-3">
            <button onClick={cancelImport} className="px-4 py-2 text-sm font-medium hover:bg-secondary rounded-xl transition-colors">Hủy bỏ</button>
            <button onClick={confirmImport} className="px-6 py-2 bg-primary text-white text-sm font-semibold rounded-xl hover:bg-primary-hover shadow-sm transition-all">Xác nhận Import</button>
          </div>
        </div>
      )}

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
