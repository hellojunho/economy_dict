export type JwtPayload = {
  sub?: string;
  role?: string;
  exp?: number;
};

export function parseJwt(token?: string | null): JwtPayload | null {
  if (!token) return null;
  const parts = token.split('.');
  if (parts.length < 2) return null;
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + (4 - (base64.length % 4)) % 4, '=');
    const json = atob(padded);
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}

export function getRoleFromToken(token?: string | null): string | null {
  const payload = parseJwt(token);
  return payload?.role ?? null;
}
