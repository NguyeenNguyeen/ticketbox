import { create } from 'zustand';
import type { OrderItem } from '@/types/order';
import type { PaymentMethod } from '@/types/order';

interface CartStore {
  items: OrderItem[];
  totalAmount: number;
  paymentMethod: PaymentMethod | null;
  isProcessing: boolean;
  holdExpiresAt: string | null;
  idempotencyKey: string | null;
  setItems: (items: OrderItem[]) => void;
  setPaymentMethod: (method: PaymentMethod) => void;
  setProcessing: (processing: boolean) => void;
  setHoldExpiresAt: (expiresAt: string | null) => void;
  setIdempotencyKey: (key: string) => void;
  clearCart: () => void;
}

export const useCartStore = create<CartStore>((set) => ({
  items: [],
  totalAmount: 0,
  paymentMethod: null,
  isProcessing: false,
  holdExpiresAt: null,
  idempotencyKey: null,

  setItems: (items) =>
    set({
      items,
      totalAmount: items.reduce((sum, item) => sum + item.price, 0),
    }),

  setPaymentMethod: (method) => set({ paymentMethod: method }),
  setProcessing: (processing) => set({ isProcessing: processing }),
  setHoldExpiresAt: (expiresAt) => set({ holdExpiresAt: expiresAt }),
  setIdempotencyKey: (key) => set({ idempotencyKey: key }),

  clearCart: () =>
    set({
      items: [],
      totalAmount: 0,
      paymentMethod: null,
      isProcessing: false,
      holdExpiresAt: null,
      idempotencyKey: null,
    }),
}));
