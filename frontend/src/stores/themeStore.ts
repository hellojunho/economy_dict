import { create } from 'zustand';

type Theme = 'dark' | 'light';

const STORAGE_KEY = 'app-theme';

function getInitialTheme(): Theme {
  return 'light';
}

function applyTheme(theme: Theme) {
  document.documentElement.removeAttribute('data-theme');
  localStorage.setItem(STORAGE_KEY, theme);
}

type ThemeStore = {
  theme: Theme;
  toggle: () => void;
};

export const useThemeStore = create<ThemeStore>((set, get) => {
  const initial = getInitialTheme();
  applyTheme(initial);

  return {
    theme: initial,
    toggle: () => {
      const next = get().theme === 'dark' ? 'light' : 'dark';
      applyTheme(next);
      set({ theme: next });
    }
  };
});
