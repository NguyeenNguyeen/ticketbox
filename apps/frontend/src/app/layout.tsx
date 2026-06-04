import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { Providers } from "./providers";

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin", "vietnamese"],
  display: "swap",
});

export const metadata: Metadata = {
  title: {
    default: "TicketBox - Nền tảng bán vé sự kiện hàng đầu Việt Nam",
    template: "%s | TicketBox",
  },
  description:
    "Mua vé concert, sự kiện âm nhạc trực tuyến dễ dàng, an toàn. Hỗ trợ thanh toán VNPAY, MoMo. Nhận e-ticket ngay lập tức.",
  keywords: [
    "mua vé concert",
    "vé sự kiện",
    "TicketBox",
    "concert Việt Nam",
    "vé online",
  ],
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="vi" className={`${inter.variable} h-full`}>
      <body className="min-h-full flex flex-col antialiased">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
