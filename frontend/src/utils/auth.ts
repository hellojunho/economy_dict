export type JwtPayload = {
  sub?: string;
  role?: string;
  exp?: number;
};

export const ACCESS_TOKEN_KEY = 'accessToken';

export function readAccessToken(): string | null {
  if (typeof window === 'undefined') {
    return null;
  }
  return window.localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function persistAccessToken(token: string) {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.setItem(ACCESS_TOKEN_KEY, token);
}

export function clearPersistedAccessToken() {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
}

export function parseJwt(token?: string | null): JwtPayload | null {
  if (!token) {
    return null;
  }
  const parts = token.split('.');
  if (parts.length < 2) {
    return null;
  }
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
  return parseJwt(token)?.role ?? null;
}

export function hasValidToken(token?: string | null): boolean {
  const payload = parseJwt(token);
  if (!payload?.exp) {
    return false;
  }
  return payload.exp * 1000 > Date.now();
}

export function getAuthSnapshot(token = readAccessToken()) {
  if (!hasValidToken(token)) {
    return {
      accessToken: null,
      role: null,
      isAuthenticated: false
    };
  }

  return {
    accessToken: token,
    role: getRoleFromToken(token),
    isAuthenticated: true
  };
}
