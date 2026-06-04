"use client";
import { useState } from "react";
import Link from "next/link";
import { useAuthStore } from "@/stores/useAuthStore";
import { Menu, X, Ticket, LogOut, ChevronDown, User } from "lucide-react";
import { Badge } from "@/components/ui/Badge";

export function Header() {
  const { isAuthenticated, user, logout } = useAuthStore();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [dropdownOpen, setDropdownOpen] = useState(false);

  const roleLabels: Record<string, string> = { CUSTOMER: "Khán giả", ORGANIZER: "Ban tổ chức", CHECKER: "Soát vé" };

  return (
    <header className="sticky top-0 z-40 bg-white/80 backdrop-blur-lg border-b border-border">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-2">
            <Ticket className="w-7 h-7 text-primary" />
            <span className="text-xl font-bold text-gradient-primary">TicketBox</span>
          </Link>

          {/* Desktop Nav */}
          <nav className="hidden md:flex items-center gap-8">
            <Link href="/" className="text-sm font-medium text-muted-foreground hover:text-primary transition-colors">Trang chủ</Link>
            <Link href="/#concerts" className="text-sm font-medium text-muted-foreground hover:text-primary transition-colors">Sự kiện</Link>
          </nav>

          {/* Desktop Auth */}
          <div className="hidden md:flex items-center gap-3">
            {isAuthenticated && user ? (
              <div className="relative">
                <button onClick={() => setDropdownOpen(!dropdownOpen)} className="flex items-center gap-2 px-3 py-2 rounded-xl hover:bg-secondary transition-colors">
                  <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center">
                    <User className="w-4 h-4 text-primary" />
                  </div>
                  <span className="text-sm font-medium">{user.name}</span>
                  <ChevronDown className="w-4 h-4 text-muted-foreground" />
                </button>
                {dropdownOpen && (
                  <div className="absolute right-0 mt-2 w-56 bg-white rounded-xl border border-border shadow-lg p-2 animate-fade-in">
                    <div className="px-3 py-2 mb-1">
                      <p className="text-sm font-medium">{user.name}</p>
                      <Badge variant="default" className="mt-1">{roleLabels[user.role] || user.role}</Badge>
                    </div>
                    <hr className="my-1 border-border" />
                    {user.role === "ORGANIZER" && (
                      <Link href="/admin" className="block px-3 py-2 text-sm rounded-lg hover:bg-secondary transition-colors" onClick={() => setDropdownOpen(false)}>
                        Trang quản trị
                      </Link>
                    )}
                    <button onClick={() => { logout(); setDropdownOpen(false); }} className="w-full text-left px-3 py-2 text-sm rounded-lg hover:bg-secondary transition-colors text-destructive flex items-center gap-2">
                      <LogOut className="w-4 h-4" /> Đăng xuất
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <>
                <Link href="/auth/login" className="text-sm font-medium px-4 py-2 rounded-xl hover:bg-secondary transition-colors">Đăng nhập</Link>
                <Link href="/auth/register" className="text-sm font-medium px-4 py-2 rounded-xl bg-primary text-white hover:bg-primary-hover transition-colors">Đăng ký</Link>
              </>
            )}
          </div>

          {/* Mobile Toggle */}
          <button className="md:hidden p-2" onClick={() => setMobileOpen(!mobileOpen)}>
            {mobileOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>
      </div>

      {/* Mobile Menu */}
      {mobileOpen && (
        <div className="md:hidden border-t border-border bg-white animate-fade-in">
          <div className="px-4 py-4 space-y-3">
            <Link href="/" className="block text-sm font-medium py-2" onClick={() => setMobileOpen(false)}>Trang chủ</Link>
            <Link href="/#concerts" className="block text-sm font-medium py-2" onClick={() => setMobileOpen(false)}>Sự kiện</Link>
            <hr className="border-border" />
            {isAuthenticated ? (
              <button onClick={() => { logout(); setMobileOpen(false); }} className="block text-sm font-medium py-2 text-destructive">Đăng xuất</button>
            ) : (
              <>
                <Link href="/auth/login" className="block text-sm font-medium py-2" onClick={() => setMobileOpen(false)}>Đăng nhập</Link>
                <Link href="/auth/register" className="block text-sm font-medium py-2 text-primary" onClick={() => setMobileOpen(false)}>Đăng ký</Link>
              </>
            )}
          </div>
        </div>
      )}
    </header>
  );
}
