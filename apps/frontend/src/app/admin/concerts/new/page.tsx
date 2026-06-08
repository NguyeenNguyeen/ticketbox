"use client";

import { ConcertForm } from "@/components/admin/ConcertForm";
import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { useToast } from "@/components/ui/Toast";

export default function NewConcertPage() {
  const router = useRouter();
  const { toast } = useToast();

  const handleSubmit = async (data: Record<string, unknown>) => {
    const res = await api.post<{ id: string }>("/admin/concerts", data);
    toast({ title: "Tạo sự kiện thành công!", variant: "success" });
    return res?.id || "1";
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

      <h1 className="text-3xl font-bold mb-2">Tạo sự kiện mới</h1>
      <p className="text-muted-foreground mb-8">
        Điền thông tin để tạo sự kiện concert mới
      </p>

      <div className="bg-white rounded-2xl border border-border p-6 md:p-8">
        <ConcertForm onSubmit={handleSubmit} />
      </div>
    </div>
  );
}
