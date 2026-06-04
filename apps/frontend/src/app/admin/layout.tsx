import { AdminSidebar } from "@/components/layout/AdminSidebar";

export const metadata = {
  title: "Quản trị",
  description: "Trang quản trị TicketBox - Quản lý sự kiện và doanh thu",
};

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen flex bg-secondary/30">
      <AdminSidebar />
      <main className="flex-1 min-h-screen lg:ml-64">
        <div className="p-6 lg:p-8">{children}</div>
      </main>
    </div>
  );
}
