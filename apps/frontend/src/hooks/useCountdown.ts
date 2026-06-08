"use client";
import { useState, useEffect, useCallback } from "react";

export function useCountdown(targetDate: string | null) {
  const [timeLeft, setTimeLeft] = useState({ minutes: 0, seconds: 0 });
  const [isExpired, setIsExpired] = useState(false);

  const calculate = useCallback(() => {
    if (!targetDate) return { minutes: 0, seconds: 0, expired: true };
    const diff = new Date(targetDate).getTime() - Date.now();
    if (diff <= 0) return { minutes: 0, seconds: 0, expired: true };
    return {
      minutes: Math.floor(diff / 60000),
      seconds: Math.floor((diff % 60000) / 1000),
      expired: false,
    };
  }, [targetDate]);

  useEffect(() => {
    const update = () => {
      const r = calculate();
      setTimeLeft({ minutes: r.minutes, seconds: r.seconds });
      setIsExpired(r.expired);
    };
    update();
    const id = setInterval(update, 1000);
    return () => clearInterval(id);
  }, [calculate]);

  const formattedTime = `${String(timeLeft.minutes).padStart(2, "0")}:${String(timeLeft.seconds).padStart(2, "0")}`;

  return { ...timeLeft, isExpired, formattedTime };
}
