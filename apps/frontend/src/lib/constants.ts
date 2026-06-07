import type { ZoneName, SeatStatus } from "@/types/seat";

export const ZONE_COLORS: Record<ZoneName, string> = {
  SVIP: "#F59E0B",
  VIP: "#7C3AED",
  CAT1: "#3B82F6",
  CAT2: "#10B981",
  GA: "#9CA3AF",
};

export const ZONE_LABELS: Record<ZoneName, string> = {
  SVIP: "SVIP - Super VIP",
  VIP: "VIP",
  CAT1: "CAT1 - Hạng 1",
  CAT2: "CAT2 - Hạng 2",
  GA: "GA - Sân khấu chung",
};

export const ZONE_PRICES: Record<ZoneName, number> = {
  SVIP: 5000000,
  VIP: 3500000,
  CAT1: 2000000,
  CAT2: 1200000,
  GA: 800000,
};

export const SEAT_STATUS_STYLES: Record<SeatStatus, { opacity: number; cursor: string; fill?: string }> = {
  available: { opacity: 1, cursor: "pointer" },
  held: { opacity: 0.3, cursor: "not-allowed" },
  taken: { opacity: 0.5, cursor: "not-allowed", fill: "#D1D5DB" },
  selected: { opacity: 1, cursor: "pointer" },
};

export const HOLD_TIMEOUT_MINUTES = 10;
export const POLLING_INTERVAL = 10000;
export const MAX_RETRY_COUNT = 3;
export const API_BASE_URL = `${process.env.NEXT_PUBLIC_API_URL}/api`;
