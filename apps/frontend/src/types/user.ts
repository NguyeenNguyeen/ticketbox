export type UserRole = 'CUSTOMER' | 'ORGANIZER' | 'CHECKER';

export interface User {
  id: string;
  name: string;
  email: string;
  phone: string;
  role: UserRole;
  avatarUrl?: string;
  createdAt: string;
}

export interface JwtPayload {
  sub: string;       // user id
  name: string;
  email: string;
  role: UserRole;
  iat: number;
  exp: number;
}

export interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
}
