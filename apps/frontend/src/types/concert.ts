export type ConcertStatus = 'UPCOMING' | 'ON_SALE' | 'SOLD_OUT' | 'COMPLETED' | 'CANCELLED';

export interface TicketCategory {
  id: string;
  name: string;           // e.g., "SVIP", "VIP", "CAT1", "CAT2", "GA"
  price: number;           // VND
  totalQuantity: number;
  availableQuantity: number;
  maxPerUser: number;
  saleStartTime: string;   // ISO datetime
  color: string;           // hex color for seat map
}

export interface Artist {
  id: string;
  name: string;
  avatarUrl: string;
  bio: string;
}

export interface Concert {
  id: string;
  title: string;
  description: string;
  artists: Artist[];
  venue: string;
  address: string;
  date: string;            // ISO datetime
  doors: string;           // e.g., "18:00"
  showTime: string;        // e.g., "19:30"
  bannerUrl: string;
  status: ConcertStatus;
  hasSeatMap: boolean;
  ticketCategories: TicketCategory[];
  createdAt: string;
  updatedAt: string;
}

export interface ConcertListItem {
  id: string;
  title: string;
  venue: string;
  date: string;
  bannerUrl: string;
  status: ConcertStatus;
  priceFrom: number;
  artists: string[];
}
