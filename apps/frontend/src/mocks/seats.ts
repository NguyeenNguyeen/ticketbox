import type { Seat, ZoneName, ZoneInfo } from "@/types/seat";
import { ZONE_COLORS, ZONE_PRICES } from "@/lib/constants";

function seededRandom(seed: number) {
  let s = seed;
  return () => {
    s = (s * 16807) % 2147483647;
    return (s - 1) / 2147483646;
  };
}

interface ZoneConfig {
  zone: ZoneName;
  rows: string[];
  seatsPerRow: number;
}

const ZONE_LAYOUT: ZoneConfig[] = [
  { zone: "SVIP", rows: ["A", "B", "C", "D"], seatsPerRow: 10 },
  { zone: "VIP", rows: ["E", "F", "G", "H", "I", "J"], seatsPerRow: 15 },
  { zone: "CAT1", rows: ["K", "L", "M", "N", "O", "P", "Q", "R", "S", "T"], seatsPerRow: 20 },
  { zone: "CAT2", rows: ["U", "V", "W", "X", "Y", "Z"], seatsPerRow: 25 },
];

export function generateSeats(concertId: string): Seat[] {
  const hash = concertId.split("").reduce((a, c) => a + c.charCodeAt(0), 0);
  const rand = seededRandom(hash);
  const seats: Seat[] = [];

  for (const zc of ZONE_LAYOUT) {
    for (const row of zc.rows) {
      for (let n = 1; n <= zc.seatsPerRow; n++) {
        const r = rand();
        let status: Seat["status"] = "available";
        if (r < 0.15) status = "taken";
        else if (r < 0.20) status = "held";

        seats.push({
          id: `${concertId}-${row}${n}`,
          row,
          number: n,
          zone: zc.zone,
          status,
          price: ZONE_PRICES[zc.zone],
        });
      }
    }
  }

  // GA standing spots
  for (let n = 1; n <= 50; n++) {
    const r = rand();
    let status: Seat["status"] = "available";
    if (r < 0.10) status = "taken";
    seats.push({
      id: `${concertId}-GA${n}`,
      row: "GA",
      number: n,
      zone: "GA",
      status,
      price: ZONE_PRICES.GA,
    });
  }

  return seats;
}

export function mockZoneInfo(concertId: string): ZoneInfo[] {
  const seats = generateSeats(concertId);
  const zones: ZoneName[] = ["SVIP", "VIP", "CAT1", "CAT2", "GA"];

  return zones.map((zone) => {
    const zoneSeats = seats.filter((s) => s.zone === zone);
    return {
      name: zone,
      label: zone,
      color: ZONE_COLORS[zone],
      price: ZONE_PRICES[zone],
      total: zoneSeats.length,
      available: zoneSeats.filter((s) => s.status === "available").length,
    };
  });
}
