import { API_BASE_URL } from "./constants";

export const API_ENDPOINTS = {
  concerts: "/concerts",
  concertDetail: (id: string) => `/concerts/${id}`,
  seats: (concertId: string) => `/concerts/${concertId}/seats`,
  orders: "/orders",
  orderDetail: (id: string) => `/orders/${id}`,
  payment: "/payments",
  auth: { login: "/auth/login", register: "/auth/register" },
  admin: { concerts: "/admin/concerts", guests: "/admin/guests", stats: "/admin/stats" },
};

function getAuthHeader(): Record<string, string> {
  if (typeof window === "undefined") return {};
  const token = localStorage.getItem("ticketbox_token");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function request<T>(method: string, url: string, body?: unknown): Promise<T> {
  const fullUrl = `${API_BASE_URL}${url}`;
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...getAuthHeader(),
  };

  try {
    const res = await fetch(fullUrl, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });

    if (!res.ok) throw new Error(`API error: ${res.status}`);
    const text = await res.text();
    try {
      return text ? JSON.parse(text) : {};
    } catch {
      return text as unknown as T;
    }
  } catch (error) {
    console.error(`Request failed: ${fullUrl}`, error);
    throw error;
  }
}

export const api = {
  get: <T>(url: string) => request<T>("GET", url),
  post: <T>(url: string, body?: unknown) => request<T>("POST", url, body),
  put: <T>(url: string, body?: unknown) => request<T>("PUT", url, body),
  delete: <T>(url: string) => request<T>("DELETE", url),
};
