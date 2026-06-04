"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { LayoutDashboard, Calendar, Users, Ticket, Menu, X, LogOut } from "lucide-react";
import { useState } from "react";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/stores/useAuthStore";

const navItems = [
  { label: "Dashboard", href: "/admin", icon: LayoutDashboard },
  { label: "Sự kiện", href: "/admin/concerts", icon: Calendar },
  { label: "Khách mời VIP", href: "/admin/guests", icon: Users },
];

export function AdminSidebar() {
  const pathname = usePathname();
  const [open, setOpen] = useState(false);
  const logout = useAuthStore((s) => s.logout);

  const isActive = (href: string) => href === "/admin" ? pathname === "/admin" : pathname.startsWith(href);

  const sidebar = (
    <div className="flex flex-col h-full">
      <div className="p-6">
        <Link href="/admin" className="flex items-center gap-2">
          <Ticket className="w-7 h-7 text-primary" />
          <span className="text-lg font-bold text-gradient-primary">TicketBox</span>
          <span className="text-xs font-medium text-muted-foreground ml-1">Admin</span>
        </Link>
      </div>
      <nav className="flex-1 px-3 space-y-1">
        {navItems.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            onClick={() => setOpen(false)}
            className={cn(
              "flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all",
              isActive(item.href) ? "bg-primary text-white shadow-sm" : "text-muted-foreground hover:bg-secondary hover:text-foreground"
            )}
          >
            <item.icon className="w-5 h-5" />
            {item.label}
          </Link>
        ))}
      </nav>
      <div className="p-3 border-t border-border">
        <Link href="/" className="flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium text-muted-foreground hover:bg-secondary transition-all">
          ← Về trang chủ
        </Link>
        <button onClick={logout} className="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium text-destructive hover:bg-destructive/10 transition-all">
          <LogOut className="w-5 h-5" /> Đăng xuất
        </button>
      </div>
    </div>
  );

  return (
    <>
      {/* Mobile toggle */}
      <button onClick={() => setOpen(true)} className="lg:hidden fixed top-4 left-4 z-50 p-2 bg-white rounded-xl border border-border shadow-sm">
        <Menu className="w-5 h-5" />
      </button>
      {/* Mobile overlay */}
      {open && <div className="lg:hidden fixed inset-0 z-40 bg-black/40" onClick={() => setOpen(false)} />}
      {/* Sidebar */}
      <aside className={cn(
        "fixed top-0 left-0 h-full w-64 bg-white border-r border-border z-50 transition-transform lg:translate-x-0",
        open ? "translate-x-0" : "-translate-x-full lg:translate-x-0"
      )}>
        <button onClick={() => setOpen(false)} className="lg:hidden absolute top-4 right-4 p-1">
          <X className="w-5 h-5" />
        </button>
        {sidebar}
      </aside>
    </>
  );
}
