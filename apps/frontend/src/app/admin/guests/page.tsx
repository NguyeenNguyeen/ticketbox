"use client";

import { Users, Search, Download, UploadCloud, X, CheckCircle2, AlertTriangle, Trash2 } from "lucide-react";
import { useState, useRef, useEffect } from "react";
import { api } from "@/lib/api";
import { useToast } from "@/components/ui/Toast";

interface GuestEntry {
  id: string;
  name: string;
  email: string;
  phone: string;
  concert: string;
  sponsor: string;
  status: "PENDING" | "CONFIRMED" | "CHECKED_IN";
  importedAt: string;
}

export default function AdminGuestsPage() {
  const [guests, setGuests] = useState<GuestEntry[]>([]);
  const [search, setSearch] = useState("");
  const [filterConcert, setFilterConcert] = useState("all");
  const [isImporting, setIsImporting] = useState(false);
  const [csvFile, setCsvFile] = useState<File | null>(null);
  const [previewData, setPreviewData] = useState<{row: number, data: string[], error: string}[]>([]);
  const [loading, setLoading] = useState(true);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const { toast } = useToast();

  const loadGuests = async () => {
    try {
      setLoading(true);
      const params = new URLSearchParams();
      if (search) params.append("search", search);
      if (filterConcert && filterConcert !== "all") params.append("concertName", filterConcert);
      
      const res = await api.get<GuestEntry[]>(`/admin/guests?${params.toString()}`);
      setGuests(res);
    } catch (err) {
      toast({ title: "Lỗi", description: "Không thể tải danh sách khách mời", variant: "error" });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadGuests();
  }, [search, filterConcert]);

  const concerts = [...new Set(guests.map((g) => g.concert))];

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setCsvFile(file);
      
      const reader = new FileReader();
      reader.onload = (event) => {
        const text = event.target?.result as string;
        if (text) {
          const lines = text.split('\n').filter(line => line.trim().length > 0);
          const preview = lines.slice(1, 6).map((line, index) => {
            const cols = line.split(',');
            let error = "";
            if (cols.length < 2 || !cols[1]?.includes('@')) {
              error = "Email không hợp lệ hoặc bị thiếu";
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

  const confirmImport = async () => {
    if (!csvFile) return;
    try {
      const formData = new FormData();
      formData.append("file", csvFile);
      
      const token = localStorage.getItem("ticketbox_token");
      const res = await fetch("http://localhost:8080/api/admin/guests/import", {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: formData,
      });
      
      const resData = await res.json();
      if (!res.ok) {
        throw new Error(resData.error || "Import failed");
      }
      
      toast({ title: "Thành công", description: resData.message || "Đã import danh sách khách mời thành công", variant: "success" });
      cancelImport();
      loadGuests();
    } catch (error: any) {
      toast({ title: "Lỗi", description: error.message, variant: "error" });
    }
  };

  const handleDeleteGuest = async (id: string, name: string) => {
    if (!confirm(`Bạn có chắc muốn xoá khách mời ${name}? Hành động này không thể hoàn tác.`)) {
      return;
    }
    
    try {
      const numericId = id.replace('g', ''); // Strip the 'g' prefix added by the backend DTO
      await api.delete(`/admin/guests/${numericId}`);
      toast({ title: "Thành công", description: "Đã xoá khách mời", variant: "success" });
      loadGuests();
    } catch (err: any) {
      toast({ title: "Lỗi", description: "Không thể xoá khách mời", variant: "error" });
    }
  };

  const handleConfirmGuest = async (id: string, name: string) => {
    try {
      const numericId = id.replace('g', '');
      await api.put(`/admin/guests/${numericId}/confirm`);
      toast({ title: "Thành công", description: `Đã xác nhận khách mời ${name}`, variant: "success" });
      loadGuests();
    } catch (err: any) {
      toast({ title: "Lỗi", description: "Không thể xác nhận khách mời", variant: "error" });
    }
  };

  const statusMap: Record<string, { label: string; color: string }> = {
    CONFIRMED: { label: "Đã xác nhận", color: "bg-success/10 text-success" },
    PENDING: { label: "Chờ xác nhận", color: "bg-warning/10 text-warning" },
    CHECKED_IN: { label: "Đã check-in", color: "bg-blue-50 text-blue-600" },
  };

  const handleExportCsv = () => {
    if (guests.length === 0) {
      alert("Không có dữ liệu để xuất");
      return;
    }

    const headers = ["Họ tên", "Email", "SĐT", "Sự kiện", "Nhãn hàng", "Trạng thái"];
    const rows = guests.map(g => [
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
            type="button"
            onClick={() => {
              if (fileInputRef.current) fileInputRef.current.value = "";
              fileInputRef.current?.click();
            }}
            className="inline-flex items-center gap-2 bg-primary text-white font-medium px-5 py-2.5 rounded-xl hover:bg-primary-hover transition-all shadow-sm"
          >
            <UploadCloud className="w-5 h-5" />
            Import CSV
          </button>
          <input 
            type="file" 
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
                <th className="text-right px-6 py-4 text-sm font-semibold">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {guests.map((guest) => (
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
                      className={`text-xs font-medium px-3 py-1 rounded-full ${statusMap[guest.status]?.color || 'bg-secondary text-foreground'}`}
                    >
                      {statusMap[guest.status]?.label || guest.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right flex items-center justify-end gap-1">
                    {guest.status === "PENDING" && (
                      <button
                        onClick={() => handleConfirmGuest(guest.id, guest.name)}
                        className="p-2 text-muted-foreground hover:text-success hover:bg-success/10 rounded-lg transition-colors"
                        title="Xác nhận khách mời"
                      >
                        <CheckCircle2 className="w-4 h-4" />
                      </button>
                    )}
                    <button
                      onClick={() => handleDeleteGuest(guest.id, guest.name)}
                      className="p-2 text-muted-foreground hover:text-destructive hover:bg-destructive/10 rounded-lg transition-colors"
                      title="Xoá khách mời"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {!loading && guests.length === 0 && (
          <div className="text-center py-12 text-muted-foreground">
            <Users className="w-12 h-12 mx-auto mb-3 opacity-50" />
            <p>Không tìm thấy khách mời nào</p>
          </div>
        )}
        
        {loading && (
          <div className="text-center py-12 text-muted-foreground">
            <p>Đang tải dữ liệu...</p>
          </div>
        )}
      </div>

      <div className="text-sm text-muted-foreground mt-4">
        Hiển thị {guests.length} khách mời
      </div>
    </div>
  );
}
