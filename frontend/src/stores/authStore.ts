import { create } from 'zustand';
import { clearPersistedAccessToken, getAuthSnapshot, persistAccessToken, readAccessToken } from '../utils/auth';

type AuthState = {
  accessToken: string | null;
  role: string | null;
  isAuthenticated: boolean;
  hydrateAuth: () => void;
  setAccessToken: (token: string) => void;
  clearSession: () => void;
};

const initialState = getAuthSnapshot(readAccessToken());

export const useAuthStore = create<AuthState>((set) => ({
  ...initialState,
  hydrateAuth: () => {
    const snapshot = getAuthSnapshot(readAccessToken());
    if (!snapshot.accessToken) {
      clearPersistedAccessToken();
    }
    set(snapshot);
  },
  setAccessToken: (token) => {
    persistAccessToken(token);
    set(getAuthSnapshot(token));
  },
  clearSession: () => {
    clearPersistedAccessToken();
    set({ accessToken: null, role: null, isAuthenticated: false });
  }
}));
