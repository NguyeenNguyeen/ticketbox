import { Ticket } from "lucide-react";
import Link from "next/link";

export function Footer() {
  return (
    <footer className="bg-secondary/60 border-t border-border mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          <div className="md:col-span-2">
            <Link href="/" className="flex items-center gap-2 mb-3">
              <Ticket className="w-6 h-6 text-primary" />
              <span className="text-lg font-bold text-gradient-primary">TicketBox</span>
            </Link>
            <p className="text-sm text-muted-foreground max-w-sm">
              Nền tảng bán vé sự kiện hàng đầu Việt Nam. Mua vé an toàn, nhận e-ticket ngay lập tức.
            </p>
          </div>
          <div>
            <h4 className="font-semibold text-sm mb-3">Về chúng tôi</h4>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li><Link href="#" className="hover:text-primary transition-colors">Giới thiệu</Link></li>
              <li><Link href="#" className="hover:text-primary transition-colors">Liên hệ</Link></li>
              <li><Link href="#" className="hover:text-primary transition-colors">Tuyển dụng</Link></li>
            </ul>
          </div>
          <div>
            <h4 className="font-semibold text-sm mb-3">Chính sách</h4>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li><Link href="#" className="hover:text-primary transition-colors">Điều khoản sử dụng</Link></li>
              <li><Link href="#" className="hover:text-primary transition-colors">Chính sách bảo mật</Link></li>
              <li><Link href="#" className="hover:text-primary transition-colors">Chính sách hoàn vé</Link></li>
            </ul>
          </div>
        </div>
        <div className="border-t border-border mt-8 pt-6 text-center text-sm text-muted-foreground">
          © 2026 TicketBox. All rights reserved.
        </div>
      </div>
    </footer>
  );
}
