"use client";
import { useEffect, useRef } from "react";
import type { SeatChangeEvent } from "@/types/seat";

export function useSSE(url: string | null, onMessage: (event: SeatChangeEvent) => void) {
  const retryCount = useRef(0);
  const maxRetries = 5;

  useEffect(() => {
    if (!url) return;

    let es: EventSource | null = null;
    let timeoutId: ReturnType<typeof setTimeout>;

    function connect() {
      es = new EventSource(url!);

      es.onmessage = (e) => {
        retryCount.current = 0;
        try {
          const data = JSON.parse(e.data) as SeatChangeEvent;
          onMessage(data);
        } catch { /* ignore parse errors */ }
      };

      es.onerror = () => {
        es?.close();
        if (retryCount.current < maxRetries) {
          const delay = Math.min(1000 * 2 ** retryCount.current, 30000);
          retryCount.current += 1;
          timeoutId = setTimeout(connect, delay);
        }
      };
    }

    connect();

    return () => {
      es?.close();
      clearTimeout(timeoutId);
    };
  }, [url, onMessage]);
}
