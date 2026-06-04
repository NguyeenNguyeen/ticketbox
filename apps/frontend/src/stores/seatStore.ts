import { create } from 'zustand';
import type { Seat, ZoneInfo, SeatStatus } from '@/types/seat';

interface SeatStore {
  seats: Record<string, Seat>;
  zones: ZoneInfo[];
  selectedSeatIds: string[];
  isLoading: boolean;
  concertId: string | null;
  loadSeats: (concertId: string) => Promise<void>;
  selectSeat: (seatId: string) => void;
  deselectSeat: (seatId: string) => void;
  updateSeatStatus: (seatId: string, status: SeatStatus) => void;
  clearSelection: () => void;
}

// Mock data generator for demo purposes
function generateMockSeats(concertId: string): { seats: Record<string, Seat>; zones: ZoneInfo[] } {
  const seats: Record<string, Seat> = {};
  const zones: ZoneInfo[] = [
    { name: 'SVIP', label: 'SVIP', color: '#F59E0B', price: 5000000, total: 40, available: 30 },
    { name: 'VIP', label: 'VIP', color: '#7C3AED', price: 3500000, total: 80, available: 55 },
    { name: 'CAT1', label: 'CAT 1', color: '#3B82F6', price: 2000000, total: 120, available: 90 },
    { name: 'CAT2', label: 'CAT 2', color: '#10B981', price: 1200000, total: 100, available: 70 },
    { name: 'GA', label: 'General Admission', color: '#9CA3AF', price: 500000, total: 60, available: 45 },
  ];

  const zoneRows: Record<string, string[]> = {
    SVIP: ['A', 'B', 'C', 'D'],
    VIP: ['E', 'F', 'G', 'H', 'I', 'J'],
    CAT1: ['K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T'],
    CAT2: ['U', 'V', 'W', 'X', 'Y', 'Z'],
    GA: ['GA'],
  };

  const zonePrices: Record<string, number> = {
    SVIP: 5000000,
    VIP: 3500000,
    CAT1: 2000000,
    CAT2: 1200000,
    GA: 500000,
  };

  for (const [zone, rows] of Object.entries(zoneRows)) {
    const seatsPerRow = zone === 'GA' ? 60 : 10;
    for (const row of rows) {
      for (let num = 1; num <= seatsPerRow; num++) {
        const id = `${concertId}-${zone}-${row}-${num}`;
        const rand = Math.random();
        let status: SeatStatus = 'available';
        if (rand < 0.15) status = 'taken';
        else if (rand < 0.2) status = 'held';

        seats[id] = {
          id,
          row,
          number: num,
          zone: zone as Seat['zone'],
          status,
          price: zonePrices[zone],
        };
      }
    }
  }

  return { seats, zones };
}

export const useSeatStore = create<SeatStore>((set, get) => ({
  seats: {},
  zones: [],
  selectedSeatIds: [],
  isLoading: false,
  concertId: null,

  loadSeats: async (concertId: string) => {
    set({ isLoading: true, concertId });
    // Simulate API call
    await new Promise((resolve) => setTimeout(resolve, 800));
    const { seats, zones } = generateMockSeats(concertId);
    set({ seats, zones, isLoading: false, selectedSeatIds: [] });
  },

  selectSeat: (seatId: string) => {
    const { seats, selectedSeatIds } = get();
    const seat = seats[seatId];
    if (!seat || seat.status !== 'available') return;

    if (selectedSeatIds.includes(seatId)) {
      // Deselect
      set({
        selectedSeatIds: selectedSeatIds.filter((id) => id !== seatId),
        seats: {
          ...seats,
          [seatId]: { ...seat, status: 'available' },
        },
      });
    } else {
      // Select
      set({
        selectedSeatIds: [...selectedSeatIds, seatId],
        seats: {
          ...seats,
          [seatId]: { ...seat, status: 'selected' },
        },
      });
    }
  },

  deselectSeat: (seatId: string) => {
    const { seats, selectedSeatIds } = get();
    const seat = seats[seatId];
    if (!seat) return;

    set({
      selectedSeatIds: selectedSeatIds.filter((id) => id !== seatId),
      seats: {
        ...seats,
        [seatId]: { ...seat, status: 'available' },
      },
    });
  },

  updateSeatStatus: (seatId: string, status: SeatStatus) => {
    const { seats } = get();
    const seat = seats[seatId];
    if (!seat) return;

    set({
      seats: {
        ...seats,
        [seatId]: { ...seat, status },
      },
    });
  },

  clearSelection: () => {
    const { seats, selectedSeatIds } = get();
    const updatedSeats = { ...seats };
    for (const id of selectedSeatIds) {
      if (updatedSeats[id]) {
        updatedSeats[id] = { ...updatedSeats[id], status: 'available' };
      }
    }
    set({ selectedSeatIds: [], seats: updatedSeats });
  },
}));
