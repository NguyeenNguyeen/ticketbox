import type { Seat } from './seat';

export type OrderStatus = 'PENDING' | 'PAYING' | 'COMPLETED' | 'CANCELLED' | 'EXPIRED';
export type PaymentMethod = 'VNPAY' | 'MOMO';

export interface OrderItem {
  seatId: string;
  zone: string;
  row: string;
  number: number;
  price: number;
}

export interface Order {
  id: string;
  concertId: string;
  concertTitle: string;
  userId: string;
  items: OrderItem[];
  totalAmount: number;
  status: OrderStatus;
  paymentMethod: PaymentMethod | null;
  holdExpiresAt: string;   // ISO datetime — countdown timer target
  idempotencyKey: string;
  createdAt: string;
  updatedAt: string;
}

export interface ETicket {
  id: string;
  orderId: string;
  concertId: string;
  concertTitle: string;
  concertDate: string;
  venue: string;
  zone: string;
  row: string;
  seatNumber: number;
  qrCode: string;          // data encoded in QR
  holderName: string;
  holderEmail: string;
}
