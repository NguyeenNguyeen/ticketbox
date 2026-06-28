"use client";

import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ConcertForm } from "@/components/admin/ConcertForm";
import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { api } from "@/lib/api";
import { useToast } from "@/components/ui/Toast";
import type { Concert } from "@/types/concert";

export default function EditConcertPage() {
  const params = useParams();
  const router = useRouter();
  const concertId = params.id as string;

  const [concert, setConcert] = useState<Concert | null>(null);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();

  useEffect(() => {
    async function fetchConcert() {
      try {
        const data = await api.get<Concert>(`/concerts/${concertId}`);
        setConcert(data);
      } catch (err) {
        console.error("Failed to load concert", err);
        toast({ title: "Lỗi", description: "Không thể tải thông tin sự kiện", variant: "error" });
      } finally {
        setLoading(false);
      }
    }
    if (concertId) fetchConcert();
  }, [concertId]);

  if (loading) {
    return (
      <div className="text-center py-12">
        <div className="w-10 h-10 border-4 border-primary/30 border-t-primary rounded-full animate-spin mx-auto mb-4" />
        <p className="text-muted-foreground">Đang tải thông tin sự kiện...</p>
      </div>
    );
  }

  if (!concert) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">Không tìm thấy sự kiện</p>
        <Link href="/admin/concerts" className="text-primary hover:underline mt-2 inline-block">
          Quay lại danh sách
        </Link>
      </div>
    );
  }

  const handleSubmit = async (data: Record<string, unknown>) => {
    const res = await api.put<{ id: string }>(`/admin/concerts/${concertId}`, data);
    toast({ title: "Cập nhật thành công", variant: "success" });
    return res?.id || concertId;
  };

  const handleCancel = async () => {
    if (
      !window.confirm(
        "Bạn có chắc chắn muốn huỷ/hoãn sự kiện này không? Hành động này không thể hoàn tác."
      )
    ) {
      return;
    }

    try {
      await api.delete(`/admin/concerts/${concertId}`);
      toast({ title: "Đã huỷ sự kiện thành công", variant: "success" });
      router.push("/admin/concerts");
    } catch (error: any) {
      toast({ title: "Lỗi", description: error.message || "Không thể huỷ sự kiện", variant: "error" });
    }
  };

  const handleResume = async () => {
    if (
      !window.confirm(
        "Bạn có chắc chắn muốn tiếp tục sự kiện đã hoãn này không?"
      )
    ) {
      return;
    }

    try {
      await api.put(`/admin/concerts/${concertId}/resume`);
      toast({ title: "Đã tiếp tục sự kiện thành công", variant: "success" });
      router.push("/admin/concerts");
    } catch (error: any) {
      toast({ title: "Lỗi", description: error.message || "Không thể tiếp tục sự kiện", variant: "error" });
    }
  };

  const handleHardDelete = async () => {
    if (
      !window.confirm(
        "CẢNH BÁO: Bạn có chắc chắn muốn XÓA HOÀN TOÀN sự kiện này không? Hành động này sẽ xóa toàn bộ dữ liệu liên quan khỏi cơ sở dữ liệu và không thể hoàn tác."
      )
    ) {
      return;
    }

    try {
      await api.delete(`/admin/concerts/${concertId}/hard`);
      toast({ title: "Đã xóa hoàn toàn sự kiện", variant: "success" });
      router.push("/admin/concerts");
    } catch (error: any) {
      toast({ title: "Lỗi", description: error.message || "Không thể xóa sự kiện (có thể đã có giao dịch mua vé)", variant: "error" });
    }
  };

  return (
    <div>
      <Link
        href="/admin/concerts"
        className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-primary transition-colors mb-6"
      >
        <ArrowLeft className="w-4 h-4" />
        Quay lại danh sách
      </Link>

      <div className="flex flex-col md:flex-row md:items-center justify-between mb-8 gap-4">
        <div>
          <h1 className="text-3xl font-bold mb-2">Chỉnh sửa sự kiện</h1>
          <p className="text-muted-foreground">{concert.title}</p>
        </div>

        {concert.status === "CANCELLED" ? (
          <div className="flex gap-2">
            <button
              onClick={handleResume}
              className="px-4 py-2 bg-success/10 text-success hover:bg-success hover:text-success-foreground rounded-xl text-sm font-medium transition-colors"
            >
              Tiếp tục sự kiện
            </button>
            <button
              onClick={handleHardDelete}
              className="px-4 py-2 bg-destructive text-destructive-foreground hover:bg-red-700 rounded-xl text-sm font-medium transition-colors"
            >
              Xóa hoàn toàn
            </button>
          </div>
        ) : (
          <button
            onClick={handleCancel}
            className="px-4 py-2 bg-destructive/10 text-destructive hover:bg-destructive hover:text-destructive-foreground rounded-xl text-sm font-medium transition-colors"
          >
            Huỷ / Hoãn sự kiện
          </button>
        )}
      </div>

      <div className="bg-white rounded-2xl border border-border p-6 md:p-8">
        <ConcertForm initialData={concert} onSubmit={handleSubmit} />
      </div>
    </div>
  );
}
