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

export class ApiError extends Error {
  status?: number;
  constructor(message: string, status?: number) {
    super(message);
    this.status = status;
  }
}

async function request<T>(method: string, url: string, body?: unknown, customHeaders?: Record<string, string>): Promise<T> {
  if (typeof window !== "undefined" && !navigator.onLine) {
    throw new ApiError("Không có kết nối Internet. Vui lòng kiểm tra lại mạng.", 0);
  }

  const fullUrl = `${API_BASE_URL}${url}`;
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...getAuthHeader(),
    ...customHeaders,
  };

  try {
    const res = await fetch(fullUrl, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });

    if (!res.ok) throw new ApiError(`API error: ${res.status}`, res.status);
    const text = await res.text();
    try {
      return text ? JSON.parse(text) : ({} as unknown as T);
    } catch {
      return text as unknown as T;
    }
  } catch (error: any) {
    console.error(`Request failed: ${fullUrl}`, error);
    // Network errors (like CORS, timeout, unreachable)
    if (error.name === "TypeError" && error.message === "Failed to fetch") {
      throw new ApiError("Không thể kết nối đến máy chủ. Vui lòng thử lại sau.", 0);
    }
    throw error;
  }
}

export const api = {
  get: <T>(url: string, headers?: Record<string, string>) => request<T>("GET", url, undefined, headers),
  post: <T>(url: string, body?: unknown, headers?: Record<string, string>) => request<T>("POST", url, body, headers),
  put: <T>(url: string, body?: unknown, headers?: Record<string, string>) => request<T>("PUT", url, body, headers),
  delete: <T>(url: string, headers?: Record<string, string>) => request<T>("DELETE", url, undefined, headers),
  uploadFile: async <T>(url: string, file: File, fieldName: string = "file") => {
    const fullUrl = `${API_BASE_URL}${url}`;
    const formData = new FormData();
    formData.append(fieldName, file);
    
    const headers: Record<string, string> = {
      ...getAuthHeader(),
    };
    
    const res = await fetch(fullUrl, {
      method: "POST",
      headers,
      body: formData,
    });
    
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new ApiError(err.error || `Upload failed: ${res.status}`, res.status);
    }
    return res.json() as Promise<T>;
  }
};
