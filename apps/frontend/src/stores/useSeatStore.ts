import { create } from "zustand";
import type { Seat, SeatStatus } from "@/types/seat";
import { generateSeats } from "@/mocks/seats";
import { api } from "@/lib/api";

interface CategoryInfo {
  maxPerUser: number;
  price: number;
}

interface SeatState {
  seats: Record<string, Seat>;
  selectedSeats: string[];
  categoriesMap: Record<string, CategoryInfo>;
  loading: boolean;
  loadSeats: (concertId: string) => Promise<void>;
  selectSeat: (seatId: string) => void;
  updateSeatStatus: (seatId: string, status: SeatStatus) => void;
  clearSelection: () => void;
  getSelectedSeatsData: () => Seat[];
}

export const useSeatStore = create<SeatState>((set, get) => ({
  seats: {},
  selectedSeats: [],
  categoriesMap: {},
  loading: false,

  loadSeats: async (concertId: string) => {
    set({ loading: true });
    try {
      const categories = await api.get<any[]>(`/concerts/${concertId}/categories`);
      const seatList = generateSeats(concertId);
      
      const newCategoriesMap: Record<string, CategoryInfo> = {};
      for (const cat of categories) {
        newCategoriesMap[cat.name.toUpperCase()] = { maxPerUser: cat.maxPerUser || 2, price: cat.price };
      }
      
      const zoneTakenCount = new Map<string, number>();
      const zoneSeats = new Map<string, Seat[]>();
      
      for (const cat of categories) {
        const name = cat.name.toUpperCase();
        const taken = Math.max(0, cat.totalQuantity - cat.availableQuantity);
        zoneTakenCount.set(name, taken);
        zoneSeats.set(name, []);
      }
      
      for (const seat of seatList) {
        const zone = seat.zone.toUpperCase();
        if (!zoneSeats.has(zone)) {
          zoneSeats.set(zone, []);
        }
        zoneSeats.get(zone)!.push(seat);
      }
      
      const updatedSeats: Seat[] = [];
      
      for (const [zone, seatsInZone] of zoneSeats.entries()) {
        const catInfo = newCategoriesMap[zone];
        const price = catInfo ? catInfo.price : 100000;
        const takenLimit = zoneTakenCount.get(zone) || 0;
        
        for (let i = 0; i < seatsInZone.length; i++) {
          const seat = seatsInZone[i];
          seat.price = price;
          
          if (i < takenLimit) {
            seat.status = "taken";
          } else {
            seat.status = "available";
          }
          updatedSeats.push(seat);
        }
      }
      
      for (const seat of seatList) {
        if (!updatedSeats.find(s => s.id === seat.id)) {
          updatedSeats.push(seat);
        }
      }
      
      const seatMap: Record<string, Seat> = {};
      for (const s of updatedSeats) {
        seatMap[s.id] = s;
      }
      
      set({ seats: seatMap, categoriesMap: newCategoriesMap, selectedSeats: [], loading: false });
    } catch (error) {
      console.error("Failed to load seats from backend categories", error);
      const seatList = generateSeats(concertId);
      const seatMap: Record<string, Seat> = {};
      for (const s of seatList) {
        seatMap[s.id] = s;
      }
      set({ seats: seatMap, categoriesMap: {}, selectedSeats: [], loading: false });
    }
  },

  selectSeat: (seatId: string) => {
    const state = get();
    const seat = state.seats[seatId];
    if (!seat) return;

    if (seat.status === "taken" || seat.status === "held") return;

    if (seat.status === "selected") {
      set((state) => ({
        seats: { ...state.seats, [seatId]: { ...seat, status: "available" } },
        selectedSeats: state.selectedSeats.filter((id) => id !== seatId),
      }));
    } else {
      // Check limits before selecting
      const zone = seat.zone.toUpperCase();
      const catInfo = state.categoriesMap[zone];
      const maxPerUser = catInfo ? catInfo.maxPerUser : 2; // Default fallback 2
      
      const currentSelectedInZone = state.selectedSeats.filter(
        id => state.seats[id] && state.seats[id].zone.toUpperCase() === zone
      ).length;

      if (currentSelectedInZone >= maxPerUser) {
        if (typeof window !== "undefined") {
          window.alert(`Bạn chỉ được chọn tối đa ${maxPerUser} ghế cho hạng vé ${zone}!`);
        }
        return;
      }

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
