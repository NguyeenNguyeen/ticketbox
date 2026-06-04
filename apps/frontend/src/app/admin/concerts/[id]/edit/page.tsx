"use client";

import { useParams, useRouter } from "next/navigation";
import { ConcertForm } from "@/components/admin/ConcertForm";
import { mockConcerts } from "@/mocks/concerts";
import { ArrowLeft } from "lucide-react";
import Link from "next/link";

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

  const handleSubmit = async (data: Record<string, unknown>) => {
    console.log("Updating concert:", concertId, data);
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

      <h1 className="text-3xl font-bold mb-2">Chỉnh sửa sự kiện</h1>
      <p className="text-muted-foreground mb-8">{concert.title}</p>

      <div className="bg-white rounded-2xl border border-border p-6 md:p-8">
        <ConcertForm initialData={concert} onSubmit={handleSubmit} />
      </div>
    </div>
  );
}
