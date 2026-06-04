"use client";
import React from "react";
import { useSeatStore } from "@/stores/useSeatStore";
import { ZONE_COLORS } from "@/lib/constants";
import { formatCurrency } from "@/lib/utils";
import type { ZoneName } from "@/types/seat";

interface SeatProps {
  seatId: string;
  cx: number;
  cy: number;
}

export const Seat = React.memo(function Seat({ seatId, cx, cy }: SeatProps) {
  const seat = useSeatStore((s) => s.seats[seatId]);
  const selectSeat = useSeatStore((s) => s.selectSeat);
  const [hovered, setHovered] = React.useState(false);

  if (!seat) return null;

  const zoneColor = ZONE_COLORS[seat.zone as ZoneName] || "#9CA3AF";
  const isTaken = seat.status === "taken";
  const isHeld = seat.status === "held";
  const isSelected = seat.status === "selected";
  const isClickable = seat.status === "available" || isSelected;

  let fill = zoneColor;
  let opacity = 1;
  let strokeWidth = 0;
  let stroke = "none";

  if (isTaken) { fill = "#D1D5DB"; opacity = 0.5; }
  else if (isHeld) { opacity = 0.3; }
  else if (isSelected) { stroke = "#ffffff"; strokeWidth = 2.5; }

  return (
    <g>
      <circle
        cx={cx} cy={cy} r={7}
        fill={fill} opacity={opacity}
        stroke={stroke} strokeWidth={strokeWidth}
        style={{ cursor: isClickable ? "pointer" : "not-allowed", transition: "all 0.2s ease" }}
        filter={isSelected ? "url(#glow)" : undefined}
        onClick={() => isClickable && selectSeat(seatId)}
        onMouseEnter={() => isClickable && setHovered(true)}
        onMouseLeave={() => setHovered(false)}
      />
      {/* Tooltip */}
      {hovered && (
        <g>
          <rect x={cx - 55} y={cy - 50} width={110} height={38} rx={6} fill="white" stroke="#e5e7eb" strokeWidth={1} style={{ filter: "drop-shadow(0 2px 4px rgba(0,0,0,0.1))" }} />
          <text x={cx} y={cy - 34} textAnchor="middle" fontSize={10} fontWeight={600} fill="#1e293b">
            {seat.zone} - {seat.row}{seat.number}
          </text>
          <text x={cx} y={cy - 20} textAnchor="middle" fontSize={9} fill="#64748b">
            {formatCurrency(seat.price)}
          </text>
        </g>
      )}
    </g>
  );
});
