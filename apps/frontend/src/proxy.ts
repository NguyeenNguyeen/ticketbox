import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

// Routes chỉ dành cho CUSTOMER — ORGANIZER bị chặn
const CUSTOMER_ONLY_PATHS = [
  /^\/$/,                   // / (trang chủ)
  /^\/concerts\/[^/]+$/,   // /concerts/:id  (trang chi tiết + mua vé)
  /^\/checkout/,            // /checkout
  /^\/payment/,             // /payment/*
  /^\/tickets/,             // /tickets/*
  /^\/orders/,              // /orders/*
];

function decodePayload(token: string): Record<string, any> | null {
  try {
    const parts = token.split(".");
    if (parts.length !== 3) return null;
    return JSON.parse(Buffer.from(parts[1], "base64").toString("utf-8"));
  } catch {
    return null;
  }
}

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const token = request.cookies.get("token")?.value;

  /* ── Bảo vệ /admin/* ─────────────────────────────────── */
  if (pathname.startsWith("/admin")) {
    if (!token) {
      const loginUrl = new URL("/auth/login", request.url);
      loginUrl.searchParams.set("redirect", pathname);
      return NextResponse.redirect(loginUrl);
    }

    const payload = decodePayload(token);

    if (!payload) {
      const loginUrl = new URL("/auth/login", request.url);
      loginUrl.searchParams.set("redirect", pathname);
      return NextResponse.redirect(loginUrl);
    }

    // Hết hạn token
    if (payload.exp && payload.exp * 1000 < Date.now()) {
      const loginUrl = new URL("/auth/login", request.url);
      loginUrl.searchParams.set("redirect", pathname);
      return NextResponse.redirect(loginUrl);
    }

    // Chỉ ORGANIZER mới vào được /admin
    if (payload.role !== "ORGANIZER") {
      return NextResponse.redirect(new URL("/", request.url));
    }

    return NextResponse.next();
  }

  /* ── Chặn ORGANIZER vào trang khách hàng ──────────────── */
  const isCustomerOnlyPath = CUSTOMER_ONLY_PATHS.some((pattern) =>
    pattern.test(pathname)
  );

  if (isCustomerOnlyPath && token) {
    const payload = decodePayload(token);
    if (payload && payload.role === "ORGANIZER") {
      // Redirect admin về trang quản lý
      return NextResponse.redirect(new URL("/admin", request.url));
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    "/",
    "/admin/:path*",
    "/concerts/:id",
    "/checkout",
    "/checkout/:path*",
    "/payment/:path*",
    "/tickets/:path*",
    "/orders/:path*",
  ],
};
