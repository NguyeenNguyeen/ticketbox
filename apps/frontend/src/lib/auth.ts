import type { JwtPayload, UserRole } from "@/types/user";

export function encodeJwt(payload: object): string {
  const header = { alg: "HS256", typ: "JWT" };
  const h = btoa(JSON.stringify(header));
  const p = btoa(JSON.stringify(payload));
  const sig = btoa("mock-signature");
  return `${h}.${p}.${sig}`;
}

export function decodeJwt(token: string): JwtPayload | null {
  try {
    const parts = token.split(".");
    if (parts.length !== 3) return null;
    
    // Fix Base64Url encoding
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const binary = atob(base64);
    
    // Convert to UTF-8 properly
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    const decodedPayload = new TextDecoder().decode(bytes);
    
    return JSON.parse(decodedPayload) as JwtPayload;
  } catch {
    return null;
  }
}

export function isTokenExpired(token: string): boolean {
  const payload = decodeJwt(token);
  if (!payload || !payload.exp) return true;
  return payload.exp * 1000 < Date.now();
}

export function getMockToken(role: UserRole): string {
  const now = Math.floor(Date.now() / 1000);
  const users: Record<UserRole, { id: string; name: string; email: string }> = {
    CUSTOMER: { id: "user-001", name: "Nguyễn Văn A", email: "customer@ticketbox.vn" },
    ORGANIZER: { id: "user-002", name: "Trần Thị B", email: "organizer@ticketbox.vn" },
    CHECKER: { id: "user-003", name: "Lê Văn C", email: "checker@ticketbox.vn" },
  };
  const u = users[role];
  return encodeJwt({
    sub: u.id,
    name: u.name,
    email: u.email,
    role,
    iat: now,
    exp: now + 86400,
  });
}
