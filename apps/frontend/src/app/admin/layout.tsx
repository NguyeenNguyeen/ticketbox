import { AdminSidebar } from "@/components/layout/AdminSidebar";
import { AdminHeader } from "@/components/layout/AdminHeader";

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
      <div className="flex-1 flex flex-col min-h-screen lg:ml-64">
        <AdminHeader />
        <main className="flex-1 p-6 lg:p-8">{children}</main>
      </div>
    </div>
  );
}
