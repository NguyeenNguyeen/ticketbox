import { create } from "zustand";
import type { User, UserRole } from "@/types/user";
import { decodeJwt, getMockToken } from "@/lib/auth";

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<boolean>;
  loginAs: (role: UserRole) => void;
  logout: () => void;
  hydrate: () => void;
}

const STORAGE_KEY = "ticketbox_token";

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: null,
  isAuthenticated: false,

  login: async (email: string, _password: string) => {
    await new Promise((r) => setTimeout(r, 500));
    // Mock: any credentials work, role based on email
    let role: UserRole = "CUSTOMER";
    if (email.includes("admin") || email.includes("organizer")) role = "ORGANIZER";
    else if (email.includes("checker")) role = "CHECKER";

    const token = getMockToken(role);
    localStorage.setItem(STORAGE_KEY, token);
    document.cookie = `token=${token}; path=/; max-age=86400`;
    const payload = decodeJwt(token)!;
    set({
      token,
      isAuthenticated: true,
      user: {
        id: payload.sub,
        name: payload.name,
        email: payload.email,
        phone: "0912345678",
        role: payload.role,
        createdAt: new Date().toISOString(),
      },
    });
    return true;
  },

  loginAs: (role: UserRole) => {
    const token = getMockToken(role);
    localStorage.setItem(STORAGE_KEY, token);
    document.cookie = `token=${token}; path=/; max-age=86400`;
    const payload = decodeJwt(token)!;
    set({
      token,
      isAuthenticated: true,
      user: {
        id: payload.sub,
        name: payload.name,
        email: payload.email,
        phone: "0912345678",
        role: payload.role,
        createdAt: new Date().toISOString(),
      },
    });
  },

  logout: () => {
    localStorage.removeItem(STORAGE_KEY);
    document.cookie = "token=; path=/; max-age=0";
    set({ user: null, token: null, isAuthenticated: false });
  },

  hydrate: () => {
    if (typeof window === "undefined") return;
    const token = localStorage.getItem(STORAGE_KEY);
    if (!token) return;
    const payload = decodeJwt(token);
    if (!payload || payload.exp * 1000 < Date.now()) {
      localStorage.removeItem(STORAGE_KEY);
      return;
    }
    set({
      token,
      isAuthenticated: true,
      user: {
        id: payload.sub,
        name: payload.name,
        email: payload.email,
        phone: "0912345678",
        role: payload.role,
        createdAt: new Date().toISOString(),
      },
    });
  },
}));
