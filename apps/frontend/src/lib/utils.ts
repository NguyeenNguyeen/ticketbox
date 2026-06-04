import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatCurrency(amount: number): string {
  return amount.toLocaleString("vi-VN") + " ₫";
}

export function formatDate(date: string): string {
  const d = new Date(date);
  const day = d.getDate();
  const month = d.getMonth() + 1;
  const year = d.getFullYear();
  const months = [
    "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
    "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12",
  ];
  return `${day} ${months[month - 1]}, ${year}`;
}

export function formatTime(time: string): string {
  if (time.includes("T")) {
    const d = new Date(time);
    return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
  }
  return time;
}

export function generateIdempotencyKey(): string {
  return crypto.randomUUID();
}

export function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    UPCOMING: "Sắp mở bán",
    ON_SALE: "Đang bán",
    SOLD_OUT: "Hết vé",
    COMPLETED: "Đã diễn ra",
    CANCELLED: "Đã hủy",
  };
  return map[status] || status;
}

export function getStatusColor(status: string): string {
  const map: Record<string, string> = {
    UPCOMING: "bg-blue-50 text-blue-600",
    ON_SALE: "bg-green-50 text-green-600",
    SOLD_OUT: "bg-red-50 text-red-600",
    COMPLETED: "bg-gray-100 text-gray-500",
    CANCELLED: "bg-red-50 text-red-500",
  };
  return map[status] || "bg-gray-100 text-gray-500";
}
