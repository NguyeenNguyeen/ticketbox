import { create } from "zustand";
import type { OrderItem, PaymentMethod } from "@/types/order";
import { generateIdempotencyKey } from "@/lib/utils";

interface CartState {
  items: OrderItem[];
  concertId: string | null;
  concertTitle: string | null;
  holdExpiresAt: string | null;
  paymentMethod: PaymentMethod | null;
  isProcessing: boolean;
  idempotencyKey: string | null;
  setItems: (items: OrderItem[], concertId: string, concertTitle: string) => void;
  setHoldExpiry: (expiresAt: string) => void;
  setPaymentMethod: (method: PaymentMethod) => void;
  startPayment: () => void;
  completePayment: () => void;
  cancelPayment: () => void;
  clearCart: () => void;
  getTotalAmount: () => number;
}

export const useCartStore = create<CartState>((set, get) => ({
  items: [],
  concertId: null,
  concertTitle: null,
  holdExpiresAt: null,
  paymentMethod: null,
  isProcessing: false,
  idempotencyKey: null,

  setItems: (items, concertId, concertTitle) =>
    set({ items, concertId, concertTitle }),

  setHoldExpiry: (expiresAt) => set({ holdExpiresAt: expiresAt }),

  setPaymentMethod: (method) => set({ paymentMethod: method }),

  startPayment: () =>
    set({ isProcessing: true, idempotencyKey: generateIdempotencyKey() }),

  completePayment: () =>
    set({
      items: [],
      concertId: null,
      concertTitle: null,
      holdExpiresAt: null,
      paymentMethod: null,
      isProcessing: false,
      idempotencyKey: null,
    }),

  cancelPayment: () => set({ isProcessing: false }),

  clearCart: () =>
    set({
      items: [],
      concertId: null,
      concertTitle: null,
      holdExpiresAt: null,
      paymentMethod: null,
      isProcessing: false,
      idempotencyKey: null,
    }),

  getTotalAmount: () => get().items.reduce((sum, i) => sum + i.price, 0),
}));
