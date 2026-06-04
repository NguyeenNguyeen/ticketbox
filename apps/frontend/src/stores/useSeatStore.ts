import { create } from "zustand";
import type { Seat, SeatStatus } from "@/types/seat";
import { generateSeats } from "@/mocks/seats";

interface SeatState {
  seats: Record<string, Seat>;
  selectedSeats: string[];
  loading: boolean;
  loadSeats: (concertId: string) => void;
  selectSeat: (seatId: string) => void;
  updateSeatStatus: (seatId: string, status: SeatStatus) => void;
  clearSelection: () => void;
  getSelectedSeatsData: () => Seat[];
}

export const useSeatStore = create<SeatState>((set, get) => ({
  seats: {},
  selectedSeats: [],
  loading: false,

  loadSeats: (concertId: string) => {
    set({ loading: true });
    const seatList = generateSeats(concertId);
    const seatMap: Record<string, Seat> = {};
    for (const s of seatList) {
      seatMap[s.id] = s;
    }
    set({ seats: seatMap, selectedSeats: [], loading: false });
  },

  selectSeat: (seatId: string) => {
    const seat = get().seats[seatId];
    if (!seat) return;

    if (seat.status === "taken" || seat.status === "held") return;

    if (seat.status === "selected") {
      set((state) => ({
        seats: { ...state.seats, [seatId]: { ...seat, status: "available" } },
        selectedSeats: state.selectedSeats.filter((id) => id !== seatId),
      }));
    } else {
      set((state) => ({
        seats: { ...state.seats, [seatId]: { ...seat, status: "selected" } },
        selectedSeats: [...state.selectedSeats, seatId],
      }));
    }
  },

  updateSeatStatus: (seatId: string, status: SeatStatus) => {
    set((state) => {
      const seat = state.seats[seatId];
      if (!seat) return state;
      return {
        seats: { ...state.seats, [seatId]: { ...seat, status } },
        selectedSeats: status === "taken" || status === "held"
          ? state.selectedSeats.filter((id) => id !== seatId)
          : state.selectedSeats,
      };
    });
  },

  clearSelection: () => {
    set((state) => {
      const updated = { ...state.seats };
      for (const id of state.selectedSeats) {
        if (updated[id]) updated[id] = { ...updated[id], status: "available" };
      }
      return { seats: updated, selectedSeats: [] };
    });
  },

  getSelectedSeatsData: () => {
    const { seats, selectedSeats } = get();
    return selectedSeats.map((id) => seats[id]).filter(Boolean);
  },
}));
