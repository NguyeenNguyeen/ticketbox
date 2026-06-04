export type SeatStatus = 'available' | 'held' | 'taken' | 'selected';

export type ZoneName = 'SVIP' | 'VIP' | 'CAT1' | 'CAT2' | 'GA';

export interface Seat {
  id: string;
  row: string;
  number: number;
  zone: ZoneName;
  status: SeatStatus;
  price: number;
}

export interface SeatMapData {
  concertId: string;
  zones: ZoneInfo[];
  seats: Seat[];
}

export interface ZoneInfo {
  name: ZoneName;
  label: string;
  color: string;
  price: number;
  total: number;
  available: number;
}

export interface SeatChangeEvent {
  seatId: string;
  status: SeatStatus;
  timestamp: string;
}
