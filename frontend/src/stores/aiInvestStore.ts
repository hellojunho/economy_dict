import { create } from 'zustand';
import client from '../api/client';
import { AiInvestMessage, AiInvestThread, AiInvestThreadSummary } from '../components/aiInvestTypes';
import { getApiErrorMessage } from '../utils/apiError';
import { useAuthStore } from './authStore';

type AiInvestState = {
  threads: AiInvestThreadSummary[];
  activeThread: AiInvestThread | null;
  stockName: string;
  market: string;
  riskProfile: string;
  tradeStyle: string;
  notes: string;
  draft: string;
  loading: boolean;
  sidebarLoading: boolean;
  error: string;

  setStockName: (v: string) => void;
  setMarket: (v: string) => void;
  setRiskProfile: (v: string) => void;
  setTradeStyle: (v: string) => void;
  setNotes: (v: string) => void;
  setDraft: (v: string) => void;
  beginNewThread: () => void;
  reset: () => void;
  loadThreads: (autoSelectFirst?: boolean) => Promise<void>;
  selectThread: (threadId: string) => Promise<void>;
  startThread: () => Promise<void>;
  removeThread: (threadId: string) => Promise<void>;
  sendMessage: () => Promise<void>;
};

const defaultSetup = {
  stockName: '',
  market: '국내',
  riskProfile: '균형형',
  tradeStyle: '스윙',
  notes: ''
};

const baseState = {
  threads: [] as AiInvestThreadSummary[],
  activeThread: null as AiInvestThread | null,
  ...defaultSetup,
  draft: '',
  loading: false,
  sidebarLoading: false,
  error: ''
};

function applyThreadSetup(thread: AiInvestThread | null) {
  if (!thread) return defaultSetup;
  return {
    stockName: thread.stockName,
    market: thread.market,
    riskProfile: thread.riskProfile,
    tradeStyle: thread.tradeStyle,
    notes: thread.notes ?? ''
  };
}

export const useAiInvestStore = create<AiInvestState>((set, get) => ({
  ...baseState,

  setStockName: (v) => set({ stockName: v }),
  setMarket: (v) => set({ market: v }),
  setRiskProfile: (v) => set({ riskProfile: v }),
  setTradeStyle: (v) => set({ tradeStyle: v }),
  setNotes: (v) => set({ notes: v }),
  setDraft: (v) => set({ draft: v }),

  beginNewThread: () =>
    set((state) => ({
      activeThread: null,
      draft: '',
      error: '',
      riskProfile: state.riskProfile || defaultSetup.riskProfile,
      tradeStyle: state.tradeStyle || defaultSetup.tradeStyle,
      market: state.market || defaultSetup.market
    })),

  reset: () => set(baseState),

  loadThreads: async (autoSelectFirst = true) => {
    if (!useAuthStore.getState().isAuthenticated) return;
    set({ sidebarLoading: true, error: '' });
    try {
      const res = await client.get<AiInvestThreadSummary[]>('/ai-invest/threads');
      const currentId = get().activeThread?.threadId;
      set({ threads: res.data });
      if (currentId && res.data.some((t) => t.threadId === currentId)) return;
      if (autoSelectFirst && res.data.length > 0) {
        await get().selectThread(res.data[0].threadId);
      } else if (!currentId) {
        set({ activeThread: null });
      }
    } catch (err) {
      set({ error: getApiErrorMessage(err, 'AI Invest 대화 목록을 불러오지 못했습니다.') });
    } finally {
      set({ sidebarLoading: false });
    }
  },

  selectThread: async (threadId) => {
    set({ loading: true, error: '' });
    try {
      const res = await client.get<AiInvestThread>(`/ai-invest/threads/${threadId}`);
      set({ activeThread: res.data, ...applyThreadSetup(res.data) });
    } catch (err) {
      set({ error: getApiErrorMessage(err, 'AI Invest 대화를 열지 못했습니다.') });
    } finally {
      set({ loading: false });
    }
  },

  startThread: async () => {
    set({ loading: true, error: '' });
    try {
      const { stockName, market, riskProfile, tradeStyle, notes } = get();
      const res = await client.post<AiInvestThread>('/ai-invest/threads', {
        stockName,
        market,
        riskProfile,
        tradeStyle,
        notes
      });
      set({ activeThread: res.data, draft: '', ...applyThreadSetup(res.data) });
      await get().loadThreads(false);
    } catch (err) {
      set({ error: getApiErrorMessage(err, 'AI Invest 분석을 시작하지 못했습니다.') });
    } finally {
      set({ loading: false });
    }
  },

  removeThread: async (threadId) => {
    set({ loading: true, error: '' });
    try {
      await client.delete(`/ai-invest/threads/${threadId}`);
      if (get().activeThread?.threadId === threadId) {
        set({ activeThread: null, draft: '', ...defaultSetup });
      }
      await get().loadThreads(false);
    } catch (err) {
      set({ error: getApiErrorMessage(err, 'AI Invest 대화를 삭제하지 못했습니다.') });
    } finally {
      set({ loading: false });
    }
  },

  sendMessage: async () => {
    const activeThread = get().activeThread;
    const draft = get().draft.trim();
    if (!activeThread || !draft) return;

    const optimistic: AiInvestMessage = {
      role: 'user',
      content: draft,
      createdAt: new Date().toISOString()
    };

    set({
      loading: true,
      error: '',
      draft: '',
      activeThread: { ...activeThread, messages: [...activeThread.messages, optimistic] }
    });

    try {
      const res = await client.post<AiInvestThread>(
        `/ai-invest/threads/${activeThread.threadId}/messages`,
        { message: draft }
      );
      set({ activeThread: res.data, ...applyThreadSetup(res.data) });
      await get().loadThreads(false);
    } catch (err) {
      set({
        error: getApiErrorMessage(err, 'AI Invest 추가 질문에 실패했습니다.'),
        activeThread,
        draft
      });
    } finally {
      set({ loading: false });
    }
  }
}));
