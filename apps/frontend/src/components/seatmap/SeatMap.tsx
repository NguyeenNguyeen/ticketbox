"use client";
import { useEffect, useMemo } from "react";
import { useSeatStore } from "@/stores/useSeatStore";
import { mockZoneInfo } from "@/mocks/seats";
import { Seat } from "./Seat";
import { ZoneLegend } from "./ZoneLegend";
import { ZONE_COLORS, API_BASE_URL, POLLING_INTERVAL } from "@/lib/constants";
import { useSSE } from "@/hooks/useSSE";
import type { ZoneName } from "@/types/seat";

interface SeatMapProps { concertId: string; }

// Zone layout configuration for SVG positioning
const ZONE_ROWS: Record<string, { zone: ZoneName; rows: string[]; seatsPerRow: number }> = {
  SVIP: { zone: "SVIP", rows: ["A", "B", "C", "D"], seatsPerRow: 10 },
  VIP: { zone: "VIP", rows: ["E", "F", "G", "H", "I", "J"], seatsPerRow: 15 },
  CAT1: { zone: "CAT1", rows: ["K", "L", "M", "N", "O", "P", "Q", "R", "S", "T"], seatsPerRow: 20 },
  CAT2: { zone: "CAT2", rows: ["U", "V", "W", "X", "Y", "Z"], seatsPerRow: 25 },
};

function getSeatPositions(concertId: string) {
  const positions: { seatId: string; cx: number; cy: number }[] = [];
  const centerX = 400;
  let currentY = 120; // Start after stage

  // Numbered zones
  for (const key of ["SVIP", "VIP", "CAT1", "CAT2"]) {
    const config = ZONE_ROWS[key];
    for (const row of config.rows) {
      const totalWidth = config.seatsPerRow * 18;
      const startX = centerX - totalWidth / 2;
      for (let n = 1; n <= config.seatsPerRow; n++) {
        // Slight curve effect
        const progress = (n - 1) / (config.seatsPerRow - 1);
        const curve = Math.sin(progress * Math.PI) * 8;
        positions.push({
          seatId: `${concertId}-${row}${n}`,
          cx: startX + (n - 1) * 18 + 9,
          cy: currentY - curve,
        });
      }
      currentY += 18;
    }
    currentY += 10; // Gap between zones
  }

  // GA standing area
  const gaStartY = currentY + 5;
  for (let n = 1; n <= 50; n++) {
    const col = (n - 1) % 25;
    const row = Math.floor((n - 1) / 25);
    positions.push({
      seatId: `${concertId}-GA${n}`,
      cx: centerX - 220 + col * 18,
      cy: gaStartY + row * 18,
    });
  }

  return positions;
}

export function SeatMap({ concertId }: SeatMapProps) {
  const loadSeats = useSeatStore((s) => s.loadSeats);
  const updateSeatStatus = useSeatStore((s) => s.updateSeatStatus);
  const loading = useSeatStore((s) => s.loading);
  const hasSeats = useSeatStore((s) => Object.keys(s.seats).length > 0);

  // Initial load and Polling
  useEffect(() => { 
    loadSeats(concertId);
    
    // Polling interval 10s for total tickets
    const interval = setInterval(() => {
      loadSeats(concertId);
    }, POLLING_INTERVAL);
    
    return () => clearInterval(interval);
  }, [concertId, loadSeats]);

  // Realtime updates via SSE
  useSSE(`${API_BASE_URL}/concerts/${concertId}/seats/stream`, (event) => {
    updateSeatStatus(event.seatId, event.status);
  });

  const positions = useMemo(() => getSeatPositions(concertId), [concertId]);
  const zones = useMemo(() => mockZoneInfo(concertId), [concertId]);

  if (loading || !hasSeats) {
    return <div className="flex items-center justify-center h-96 text-muted-foreground">Đang tải sơ đồ chỗ ngồi...</div>;
  }

  return (
    <div>
      <div className="w-full overflow-x-auto">
        <svg viewBox="0 0 800 800" className="w-full max-w-3xl mx-auto" style={{ minWidth: 500 }}>
          {/* Glow filter for selected seats */}
          <defs>
            <filter id="glow" x="-50%" y="-50%" width="200%" height="200%">
              <feGaussianBlur stdDeviation="3" result="blur" />
              <feMerge><feMergeNode in="blur" /><feMergeNode in="SourceGraphic" /></feMerge>
            </filter>
            <linearGradient id="stageGrad" x1="0%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stopColor="hsl(250,84%,54%)" />
              <stop offset="100%" stopColor="hsl(270,70%,55%)" />
            </linearGradient>
          </defs>

          {/* Stage */}
          <rect x={200} y={20} width={400} height={50} rx={25} fill="url(#stageGrad)" />
          <text x={400} y={50} textAnchor="middle" fill="white" fontSize={16} fontWeight={700}>SÂN KHẤU</text>

          {/* Zone background labels */}
          {[
            { label: "SVIP", y: 140, color: ZONE_COLORS.SVIP },
            { label: "VIP", y: 230, color: ZONE_COLORS.VIP },
            { label: "CAT1", y: 370, color: ZONE_COLORS.CAT1 },
            { label: "CAT2", y: 530, color: ZONE_COLORS.CAT2 },
            { label: "GA", y: 660, color: ZONE_COLORS.GA },
          ].map((z) => (
            <text key={z.label} x={30} y={z.y} fontSize={11} fontWeight={700} fill={z.color} opacity={0.7}>
              {z.label}
            </text>
          ))}

          {/* Seats */}
          {positions.map((p) => (
            <Seat key={p.seatId} seatId={p.seatId} cx={p.cx} cy={p.cy} />
          ))}
        </svg>
      </div>
      <ZoneLegend zones={zones} />
    </div>
  );
}
