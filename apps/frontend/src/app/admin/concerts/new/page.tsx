"use client";

import { ConcertForm } from "@/components/admin/ConcertForm";
import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";

export default function NewConcertPage() {
  const router = useRouter();

  const handleSubmit = async (data: Record<string, unknown>) => {
    // Mock API call
    console.log("Creating concert:", data);
    await new Promise((res) => setTimeout(res, 1000));
    router.push("/admin/concerts");
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
