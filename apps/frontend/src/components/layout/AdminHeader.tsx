"use client";

import { LogOut, ChevronDown, User } from "lucide-react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/stores/useAuthStore";
import { useState } from "react";
import { Badge } from "@/components/ui/Badge";

export function AdminHeader() {
  const router = useRouter();
  const logout = useAuthStore((s) => s.logout);
  const user = useAuthStore((s) => s.user);
  const [dropdownOpen, setDropdownOpen] = useState(false);

  const handleLogout = () => {
    logout();
    setDropdownOpen(false);
    router.push("/auth/login");
  };

  return (
    <header className="sticky top-0 z-40 bg-white border-b border-border px-6 h-16 flex items-center justify-end">
      {user && (
        <div className="relative">
          <button 
            onClick={() => setDropdownOpen(!dropdownOpen)} 
            className="flex items-center gap-2 px-3 py-2 rounded-xl hover:bg-secondary transition-colors"
          >
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
                <Badge variant="default" className="mt-1">Ban tổ chức</Badge>
              </div>
              <hr className="my-1 border-border" />
              <button 
                onClick={handleLogout} 
                className="w-full text-left px-3 py-2 text-sm rounded-lg hover:bg-secondary transition-colors text-destructive flex items-center gap-2"
              >
                <LogOut className="w-4 h-4" /> Đăng xuất
              </button>
            </div>
          )}
        </div>
      )}
    </header>
  );
}
