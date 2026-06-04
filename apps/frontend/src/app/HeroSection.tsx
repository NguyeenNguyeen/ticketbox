"use client";

import { Search, Sparkles, TrendingUp, Shield } from "lucide-react";
import Link from "next/link";

export function HeroSection() {
  return (
    <section className="relative overflow-hidden bg-gradient-to-br from-[hsl(250,84%,54%)] via-[hsl(270,70%,55%)] to-[hsl(290,60%,50%)] text-white">
      {/* Decorative background elements */}
      <div className="absolute inset-0">
        <div className="absolute top-10 left-10 w-72 h-72 bg-white/10 rounded-full blur-3xl" />
        <div className="absolute bottom-10 right-10 w-96 h-96 bg-purple-300/10 rounded-full blur-3xl" />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-white/5 rounded-full blur-3xl" />
      </div>

      <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 md:py-32">
        <div className="text-center max-w-4xl mx-auto">
          {/* Badge */}
          <div className="inline-flex items-center gap-2 bg-white/15 backdrop-blur-sm rounded-full px-4 py-2 mb-8 text-sm font-medium animate-fade-in">
            <Sparkles className="w-4 h-4" />
            <span>Nền tảng bán vé sự kiện #1 Việt Nam</span>
          </div>

          {/* Title */}
          <h1 className="text-4xl sm:text-5xl md:text-6xl lg:text-7xl font-bold tracking-tight mb-6 animate-fade-in-up">
            Trải nghiệm âm nhạc
            <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-yellow-300 to-orange-300">
              không giới hạn
            </span>
          </h1>

          {/* Description */}
          <p className="text-lg md:text-xl text-white/80 max-w-2xl mx-auto mb-10 animate-fade-in-up" style={{ animationDelay: "0.1s" }}>
            Khám phá và mua vé các concert hot nhất. Thanh toán an toàn, nhận
            e-ticket ngay lập tức với mã QR.
          </p>

          {/* CTA Buttons */}
          <div className="flex flex-col sm:flex-row gap-4 justify-center items-center animate-fade-in-up" style={{ animationDelay: "0.2s" }}>
            <Link
              href="#concerts"
              className="inline-flex items-center gap-2 bg-white text-[hsl(250,84%,54%)] font-semibold px-8 py-4 rounded-xl hover:bg-white/90 transition-all hover:scale-105 shadow-lg shadow-black/20"
            >
              <Search className="w-5 h-5" />
              Khám phá sự kiện
            </Link>
            <Link
              href="/auth/register"
              className="inline-flex items-center gap-2 bg-white/15 backdrop-blur-sm text-white font-semibold px-8 py-4 rounded-xl border border-white/25 hover:bg-white/25 transition-all"
            >
              Đăng ký ngay
            </Link>
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6 mt-16 max-w-3xl mx-auto animate-fade-in-up" style={{ animationDelay: "0.3s" }}>
          {[
            { label: "Sự kiện", value: "50+", icon: Sparkles },
            { label: "Vé đã bán", value: "120K+", icon: TrendingUp },
            { label: "Khán giả", value: "200K+", icon: TrendingUp },
            { label: "Bảo mật", value: "100%", icon: Shield },
          ].map((stat) => (
            <div
              key={stat.label}
              className="text-center bg-white/10 backdrop-blur-sm rounded-xl p-4"
            >
              <stat.icon className="w-5 h-5 mx-auto mb-2 text-white/70" />
              <div className="text-2xl md:text-3xl font-bold">{stat.value}</div>
              <div className="text-sm text-white/70">{stat.label}</div>
            </div>
          ))}
        </div>
      </div>

      {/* Bottom wave */}
      <div className="absolute bottom-0 left-0 right-0">
        <svg viewBox="0 0 1440 80" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path
            d="M0 40C240 80 480 0 720 40C960 80 1200 0 1440 40V80H0V40Z"
            fill="white"
          />
        </svg>
      </div>
    </section>
  );
}
