import { create } from 'zustand';
import client from '../api/client';
import { getApiErrorMessage } from '../utils/apiError';
import { useAuthStore } from './authStore';

export type ChatThreadSummary = {
  threadId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
};

export type ChatMessage = {
  role: 'user' | 'assistant';
  content: string;
  createdAt: string;
};

export type ChatThread = {
  threadId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messages: ChatMessage[];
};

type ChatState = {
  threads: ChatThreadSummary[];
  activeThread: ChatThread | null;
  draft: string;
  loading: boolean;
  sidebarLoading: boolean;
  error: string;
  setDraft: (value: string) => void;
  reset: () => void;
  loadThreads: () => Promise<void>;
  selectThread: (threadId: string) => Promise<void>;
  createThread: () => Promise<void>;
  removeThread: (threadId: string) => Promise<void>;
  sendMessage: () => Promise<void>;
};

const baseState = {
  threads: [] as ChatThreadSummary[],
  activeThread: null as ChatThread | null,
  draft: '',
  loading: false,
  sidebarLoading: false,
  error: ''
};

export const useChatStore = create<ChatState>((set, get) => ({
  ...baseState,
  setDraft: (value) => set({ draft: value }),
  reset: () => set(baseState),
  loadThreads: async () => {
    if (!useAuthStore.getState().isAuthenticated) {
      return;
    }

    set({ sidebarLoading: true, error: '' });
    try {
      const response = await client.get<ChatThreadSummary[]>('/chats');
      const currentThreadId = get().activeThread?.threadId;
      set({ threads: response.data });

      if (currentThreadId && response.data.some((item) => item.threadId === currentThreadId)) {
        return;
      }

      if (response.data.length > 0) {
        await get().selectThread(response.data[0].threadId);
      } else {
        set({ activeThread: null });
      }
    } catch (error) {
      set({ error: getApiErrorMessage(error, '대화 목록을 불러오지 못했습니다.') });
    } finally {
      set({ sidebarLoading: false });
    }
  },
  selectThread: async (threadId) => {
    set({ loading: true, error: '' });
    try {
      const response = await client.get<ChatThread>(`/chats/${threadId}`);
      set({ activeThread: response.data });
    } catch (error) {
      set({ error: getApiErrorMessage(error, '대화를 열지 못했습니다.') });
    } finally {
      set({ loading: false });
    }
  },
  createThread: async () => {
    set({ loading: true, error: '' });
    try {
      const response = await client.post<ChatThread>('/chats', {});
      set({ activeThread: response.data });
      await get().loadThreads();
    } catch (error) {
      set({ error: getApiErrorMessage(error, '새 대화를 만들지 못했습니다.') });
    } finally {
      set({ loading: false });
    }
  },
  removeThread: async (threadId) => {
    set({ loading: true, error: '' });
    try {
      await client.delete(`/chats/${threadId}`);
      if (get().activeThread?.threadId === threadId) {
        set({ activeThread: null });
      }
      await get().loadThreads();
    } catch (error) {
      set({ error: getApiErrorMessage(error, '대화를 삭제하지 못했습니다.') });
    } finally {
      set({ loading: false });
    }
  },
  sendMessage: async () => {
    const draft = get().draft.trim();
    if (!draft) {
      return;
    }

    let threadId = get().activeThread?.threadId;
    set({ loading: true, error: '' });

    try {
      if (!threadId) {
        const created = await client.post<ChatThread>('/chats', {});
        threadId = created.data.threadId;
        set({ activeThread: created.data });
      }

      const response = await client.post<ChatThread>(`/chats/${threadId}/messages`, { message: draft });
      set({ activeThread: response.data, draft: '' });
      await get().loadThreads();
    } catch (error) {
      set({ error: getApiErrorMessage(error, '대화 요청에 실패했습니다.') });
    } finally {
      set({ loading: false });
    }
  }
}));
