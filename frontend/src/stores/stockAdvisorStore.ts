import { create } from 'zustand';
import client from '../api/client';
import {
  AdvisorMessage,
  StockAdvisorThread,
  StockAdvisorThreadSummary
} from '../components/stockAdvisorTypes';
import { getApiErrorMessage } from '../utils/apiError';
import { useAuthStore } from './authStore';

type StockAdvisorState = {
  threads: StockAdvisorThreadSummary[];
  activeThread: StockAdvisorThread | null;
  symbol: string;
  riskProfile: string;
  tradeStyle: string;
  notes: string;
  draft: string;
  loading: boolean;
  sidebarLoading: boolean;
  error: string;
  setSymbol: (value: string) => void;
  setRiskProfile: (value: string) => void;
  setTradeStyle: (value: string) => void;
  setNotes: (value: string) => void;
  setDraft: (value: string) => void;
  beginNewThread: (prefillSymbol?: string) => void;
  reset: () => void;
  loadThreads: (autoSelectFirst?: boolean) => Promise<void>;
  selectThread: (threadId: string) => Promise<void>;
  startThread: () => Promise<void>;
  removeThread: (threadId: string) => Promise<void>;
  sendMessage: () => Promise<void>;
};

const defaultSetup = {
  symbol: '',
  riskProfile: '균형형',
  tradeStyle: '스윙',
  notes: ''
};

const baseState = {
  threads: [] as StockAdvisorThreadSummary[],
  activeThread: null as StockAdvisorThread | null,
  ...defaultSetup,
  draft: '',
  loading: false,
  sidebarLoading: false,
  error: ''
};

function applyThreadSetup(thread: StockAdvisorThread | null) {
  if (!thread) {
    return defaultSetup;
  }

  return {
    symbol: thread.symbol,
    riskProfile: thread.riskProfile,
    tradeStyle: thread.tradeStyle,
    notes: thread.notes ?? ''
  };
}

export const useStockAdvisorStore = create<StockAdvisorState>((set, get) => ({
  ...baseState,
  setSymbol: (value) => set({ symbol: value }),
  setRiskProfile: (value) => set({ riskProfile: value }),
  setTradeStyle: (value) => set({ tradeStyle: value }),
  setNotes: (value) => set({ notes: value }),
  setDraft: (value) => set({ draft: value }),
  beginNewThread: (prefillSymbol) =>
    set((state) => ({
      activeThread: null,
      draft: '',
      error: '',
      symbol: prefillSymbol ?? state.symbol,
      riskProfile: state.riskProfile || defaultSetup.riskProfile,
      tradeStyle: state.tradeStyle || defaultSetup.tradeStyle
    })),
  reset: () => set(baseState),
  loadThreads: async (autoSelectFirst = true) => {
    if (!useAuthStore.getState().isAuthenticated) {
      return;
    }

    set({ sidebarLoading: true, error: '' });
    try {
      const response = await client.get<StockAdvisorThreadSummary[]>('/stock-advisor/threads');
      const currentThreadId = get().activeThread?.threadId;
      set({ threads: response.data });

      if (currentThreadId && response.data.some((item) => item.threadId === currentThreadId)) {
        return;
      }

      if (autoSelectFirst && response.data.length > 0) {
        await get().selectThread(response.data[0].threadId);
      } else if (!currentThreadId) {
        set({ activeThread: null });
      }
    } catch (error) {
      set({ error: getApiErrorMessage(error, '투자 추천 대화 목록을 불러오지 못했습니다.') });
    } finally {
      set({ sidebarLoading: false });
    }
  },
  selectThread: async (threadId) => {
    set({ loading: true, error: '' });
    try {
      const response = await client.get<StockAdvisorThread>(`/stock-advisor/threads/${threadId}`);
      set({
        activeThread: response.data,
        ...applyThreadSetup(response.data)
      });
    } catch (error) {
      set({ error: getApiErrorMessage(error, '투자 추천 대화를 열지 못했습니다.') });
    } finally {
      set({ loading: false });
    }
  },
  startThread: async () => {
    set({ loading: true, error: '' });
    try {
      const response = await client.post<StockAdvisorThread>('/stock-advisor/threads', {
        symbol: get().symbol,
        riskProfile: get().riskProfile,
        tradeStyle: get().tradeStyle,
        notes: get().notes
      });

      set({
        activeThread: response.data,
        draft: '',
        ...applyThreadSetup(response.data)
      });
      await get().loadThreads(false);
    } catch (error) {
      set({ error: getApiErrorMessage(error, '투자 추천 분석을 시작하지 못했습니다.') });
    } finally {
      set({ loading: false });
    }
  },
  removeThread: async (threadId) => {
    set({ loading: true, error: '' });
    try {
      await client.delete(`/stock-advisor/threads/${threadId}`);
      if (get().activeThread?.threadId === threadId) {
        set({
          activeThread: null,
          draft: '',
          ...defaultSetup
        });
      }
      await get().loadThreads(false);
    } catch (error) {
      set({ error: getApiErrorMessage(error, '투자 추천 대화를 삭제하지 못했습니다.') });
    } finally {
      set({ loading: false });
    }
  },
  sendMessage: async () => {
    const activeThread = get().activeThread;
    const draft = get().draft.trim();
    if (!activeThread || !draft) {
      return;
    }

    const optimisticMessage: AdvisorMessage = {
      role: 'user',
      content: draft,
      createdAt: new Date().toISOString()
    };

    set({
      loading: true,
      error: '',
      draft: '',
      activeThread: {
        ...activeThread,
        messages: [...activeThread.messages, optimisticMessage]
      }
    });

    try {
      const response = await client.post<StockAdvisorThread>(
        `/stock-advisor/threads/${activeThread.threadId}/messages`,
        { message: draft }
      );
      set({
        activeThread: response.data,
        ...applyThreadSetup(response.data)
      });
      await get().loadThreads(false);
    } catch (error) {
      set({
        error: getApiErrorMessage(error, '투자 추천 추가 질문에 실패했습니다.'),
        activeThread,
        draft
      });
    } finally {
      set({ loading: false });
    }
  }
}));
