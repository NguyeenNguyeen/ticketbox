"use client";

import { useParams, useRouter } from "next/navigation";
import { ConcertForm } from "@/components/admin/ConcertForm";
import { mockConcerts } from "@/mocks/concerts";
import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { api } from "@/lib/api";
import { useToast } from "@/components/ui/Toast";

export default function EditConcertPage() {
  const params = useParams();
  const router = useRouter();
  const concertId = params.id as string;
  const concert = mockConcerts.find((c) => c.id === concertId);

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

  const { toast } = useToast();

  const handleSubmit = async (data: Record<string, unknown>) => {
    console.log("Updating concert:", concertId, data);
    await new Promise((res) => setTimeout(res, 1000));
    router.push("/admin/concerts");
  };

  const handleCancel = async () => {
    if (!window.confirm("Bạn có chắc chắn muốn huỷ/hoãn sự kiện này không? Hành động này không thể hoàn tác.")) {
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
        
        <button 
          onClick={handleCancel}
          className="px-4 py-2 bg-destructive/10 text-destructive hover:bg-destructive hover:text-destructive-foreground rounded-xl text-sm font-medium transition-colors"
        >
          Huỷ / Hoãn sự kiện
        </button>
      </div>

      <div className="bg-white rounded-2xl border border-border p-6 md:p-8">
        <ConcertForm initialData={concert} onSubmit={handleSubmit} />
      </div>
    </div>
  );
}
