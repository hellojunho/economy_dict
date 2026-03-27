import { create } from 'zustand';
import client from '../api/client';
import { getApiErrorMessage } from '../utils/apiError';

export type WordRecord = {
  id: number;
  word: string;
  meaning: string;
  englishWord?: string | null;
};

export type WordPage = {
  content: WordRecord[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

type SearchState = {
  query: string;
  listQuery: string;
  lookupResult: WordRecord | null;
  listResponse: WordPage | null;
  selectedWord: WordRecord | null;
  loading: boolean;
  message: string;
  page: number;
  setQuery: (value: string) => void;
  selectWord: (word: WordRecord | null) => void;
  initialize: () => Promise<void>;
  search: () => Promise<void>;
  changePage: (nextPage: number) => Promise<void>;
};

async function fetchWords(query: string, page: number) {
  const response = await client.get<WordPage>('/words', {
    params: {
      q: query || undefined,
      page,
      size: 12
    }
  });
  return response.data;
}

async function fetchLookup(query: string) {
  if (!query.trim()) {
    return null;
  }
  const response = await client.get<WordRecord>('/words/lookup', { params: { q: query } });
  return response.data;
}

export const useSearchStore = create<SearchState>((set, get) => ({
  query: '',
  listQuery: '',
  lookupResult: null,
  listResponse: null,
  selectedWord: null,
  loading: false,
  message: '',
  page: 0,
  setQuery: (value) => set({ query: value }),
  selectWord: (word) => set({ selectedWord: word }),
  initialize: async () => {
    set({ loading: true, message: '' });
    try {
      const listResponse = await fetchWords('', 0);
      set({
        listResponse,
        listQuery: '',
        lookupResult: null,
        selectedWord: null,
        page: 0
      });
    } catch (error) {
      set({ message: getApiErrorMessage(error, '용어 목록을 불러오지 못했습니다.') });
    } finally {
      set({ loading: false });
    }
  },
  search: async () => {
    set({ loading: true, message: '' });
    try {
      const query = get().query;
      const [listResponse, lookupResult] = await Promise.all([fetchWords(query, 0), fetchLookup(query)]);
      set({
        listQuery: query.trim(),
        listResponse,
        lookupResult,
        selectedWord: null,
        page: 0
      });
    } catch (error) {
      set({ message: getApiErrorMessage(error, '검색 요청을 처리하지 못했습니다. 잠시 후 다시 시도하세요.') });
    } finally {
      set({ loading: false });
    }
  },
  changePage: async (nextPage) => {
    set({ loading: true, message: '' });
    try {
      const listResponse = await fetchWords(get().listQuery, nextPage);
      set({ listResponse, selectedWord: null, page: nextPage });
    } catch (error) {
      set({ message: getApiErrorMessage(error, '페이지를 이동하지 못했습니다.') });
    } finally {
      set({ loading: false });
    }
  }
}));
