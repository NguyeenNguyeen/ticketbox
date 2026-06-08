"use client";
import { useCountdown } from "@/hooks/useCountdown";
import { Clock } from "lucide-react";
import { cn } from "@/lib/utils";

export function CountdownTimer({ expiresAt }: { expiresAt: string | null }) {
  const { minutes, isExpired, formattedTime } = useCountdown(expiresAt);
  const totalMin = minutes;

  if (isExpired) {
    return (
      <div className="flex items-center gap-2 p-4 bg-destructive/10 rounded-xl text-destructive font-semibold">
        <Clock className="w-5 h-5" /> Hết thời gian giữ chỗ
      </div>
    );
  }

  return (
    <div className={cn("flex items-center gap-3 p-4 rounded-xl",
      totalMin > 5 ? "bg-green-50 text-green-700" : totalMin > 2 ? "bg-amber-50 text-amber-700" : "bg-red-50 text-red-700"
    )}>
      <Clock className={cn("w-5 h-5", totalMin < 1 && "animate-pulse-glow")} />
      <div>
        <p className="text-sm font-medium">Thời gian giữ chỗ còn lại</p>
        <p className={cn("text-2xl font-bold font-mono", totalMin < 1 && "animate-pulse")}>{formattedTime}</p>
      </div>
    </div>
  );
}
