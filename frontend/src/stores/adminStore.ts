import { create } from 'zustand';
import client from '../api/client';
import { getApiErrorMessage } from '../utils/apiError';

export type SectionKey = 'overview' | 'users' | 'words' | 'uploads';

export type AdminUser = {
  id: number;
  userId: string;
  username: string;
  email?: string | null;
  role: 'GENERAL' | 'ADMIN';
  status: 'ACTIVE' | 'DEACTIVATED';
};

export type AdminWord = {
  id: number;
  word: string;
  meaning: string;
  englishWord?: string | null;
  englishMeaning?: string | null;
  source?: string | null;
};

export type UploadTask = {
  fileId: string;
  status: string;
  message: string;
  estimatedTime?: string | null;
  progressPercent?: number | null;
  errorLog?: string | null;
};

export type DailyStat = {
  targetDate: string;
  newUsersCount: number;
  loginCount: number;
  activeUsersCount: number;
};

export type Summary = {
  totalUsers: number;
  activeUsers: number;
  totalWords: number;
  recentUploads: number;
};

export type UserFormState = {
  id: number;
  userId: string;
  username: string;
  password: string;
  email: string;
  role: AdminUser['role'];
  status: AdminUser['status'];
};

export type WordFormState = {
  id: number;
  word: string;
  meaning: string;
  englishWord: string;
  englishMeaning: string;
  source: string;
};

type AdminState = {
  section: SectionKey;
  summary: Summary | null;
  stats: DailyStat[];
  users: AdminUser[];
  words: AdminWord[];
  uploads: UploadTask[];
  message: string;
  loading: boolean;
  uploading: boolean;
  selectedFile: File | null;
  userForm: UserFormState;
  wordForm: WordFormState;
  setSection: (section: SectionKey) => void;
  setSelectedFile: (file: File | null) => void;
  updateUserForm: (patch: Partial<UserFormState>) => void;
  updateWordForm: (patch: Partial<WordFormState>) => void;
  editUser: (user: AdminUser) => void;
  editWord: (word: AdminWord) => void;
  resetUserForm: () => void;
  resetWordForm: () => void;
  clearMessage: () => void;
  refreshCurrentSection: () => Promise<void>;
  loadUploads: () => Promise<void>;
  saveUser: () => Promise<void>;
  deleteUser: (id: number) => Promise<void>;
  saveWord: () => Promise<void>;
  deleteWord: (id: number) => Promise<void>;
  uploadSelectedFile: () => Promise<void>;
};

const defaultUserForm: UserFormState = {
  id: 0,
  userId: '',
  username: '',
  password: '',
  email: '',
  role: 'GENERAL',
  status: 'ACTIVE'
};

const defaultWordForm: WordFormState = {
  id: 0,
  word: '',
  meaning: '',
  englishWord: '',
  englishMeaning: '',
  source: 'MANUAL'
};

async function fetchOverview() {
  const [summaryResponse, statsResponse] = await Promise.all([
    client.get<Summary>('/admin/stats/summary'),
    client.get<DailyStat[]>('/admin/stats/daily')
  ]);

  return {
    summary: summaryResponse.data,
    stats: statsResponse.data
  };
}

async function fetchUsers() {
  const response = await client.get<AdminUser[]>('/admin/users');
  return response.data;
}

async function fetchWords() {
  const response = await client.get<AdminWord[]>('/admin/words');
  return response.data;
}

async function fetchUploads() {
  const response = await client.get<UploadTask[]>('/admin/words/uploads');
  return response.data;
}

export const useAdminStore = create<AdminState>((set, get) => ({
  section: 'overview',
  summary: null,
  stats: [],
  users: [],
  words: [],
  uploads: [],
  message: '',
  loading: false,
  uploading: false,
  selectedFile: null,
  userForm: defaultUserForm,
  wordForm: defaultWordForm,
  setSection: (section) => set({ section }),
  setSelectedFile: (file) => set({ selectedFile: file }),
  updateUserForm: (patch) => set((state) => ({ userForm: { ...state.userForm, ...patch } })),
  updateWordForm: (patch) => set((state) => ({ wordForm: { ...state.wordForm, ...patch } })),
  editUser: (user) => set({
    userForm: {
      id: user.id,
      userId: user.userId,
      username: user.username,
      password: '',
      email: user.email ?? '',
      role: user.role,
      status: user.status
    }
  }),
  editWord: (word) => set({
    wordForm: {
      id: word.id,
      word: word.word,
      meaning: word.meaning,
      englishWord: word.englishWord ?? '',
      englishMeaning: word.englishMeaning ?? '',
      source: word.source ?? 'MANUAL'
    }
  }),
  resetUserForm: () => set({ userForm: defaultUserForm }),
  resetWordForm: () => set({ wordForm: defaultWordForm }),
  clearMessage: () => set({ message: '' }),
  refreshCurrentSection: async () => {
    const { section } = get();
    set({ loading: true, message: '' });
    try {
      if (section === 'overview') {
        const { summary, stats } = await fetchOverview();
        set({ summary, stats });
      }
      if (section === 'users') {
        set({ users: await fetchUsers() });
      }
      if (section === 'words') {
        set({ words: await fetchWords() });
      }
      if (section === 'uploads') {
        set({ uploads: await fetchUploads() });
      }
    } catch (error) {
      set({ message: getApiErrorMessage(error, '관리자 데이터를 불러오지 못했습니다.') });
    } finally {
      set({ loading: false });
    }
  },
  loadUploads: async () => {
    try {
      set({ uploads: await fetchUploads() });
    } catch {
      // polling failure is intentionally silent
    }
  },
  saveUser: async () => {
    const { userForm } = get();
    set({ message: '' });
    try {
      if (userForm.id) {
        await client.put(`/admin/users/${userForm.id}`, userForm);
      } else {
        await client.post('/admin/users', userForm);
      }
      set({ userForm: defaultUserForm, users: await fetchUsers(), message: '사용자 정보가 저장되었습니다.' });
    } catch (error) {
      set({ message: getApiErrorMessage(error, '사용자 정보를 저장하지 못했습니다.') });
    }
  },
  deleteUser: async (id) => {
    try {
      await client.delete(`/admin/users/${id}`);
      const users = await fetchUsers();
      set((state) => ({
        users,
        userForm: state.userForm.id === id ? defaultUserForm : state.userForm
      }));
    } catch (error) {
      set({ message: getApiErrorMessage(error, '사용자 삭제에 실패했습니다.') });
    }
  },
  saveWord: async () => {
    const { wordForm } = get();
    set({ message: '' });
    try {
      if (wordForm.id) {
        await client.put(`/admin/words/${wordForm.id}`, wordForm);
      } else {
        await client.post('/admin/words', wordForm);
      }
      set({ wordForm: defaultWordForm, words: await fetchWords(), message: '단어 정보가 저장되었습니다.' });
    } catch (error) {
      set({ message: getApiErrorMessage(error, '단어 정보를 저장하지 못했습니다.') });
    }
  },
  deleteWord: async (id) => {
    try {
      await client.delete(`/admin/words/${id}`);
      const words = await fetchWords();
      set((state) => ({
        words,
        wordForm: state.wordForm.id === id ? defaultWordForm : state.wordForm
      }));
    } catch (error) {
      set({ message: getApiErrorMessage(error, '단어 삭제에 실패했습니다.') });
    }
  },
  uploadSelectedFile: async () => {
    const { selectedFile } = get();
    if (!selectedFile) {
      set({ message: '업로드할 파일을 선택하세요. 지원 형식: pdf, txt, xlsx, csv, json, zip. 최대 20MB까지 허용됩니다.' });
      return;
    }

    set({ uploading: true, message: '' });
    try {
      const formData = new FormData();
      formData.append('file', selectedFile);
      await client.post('/admin/words/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      set({
        selectedFile: null,
        uploads: await fetchUploads(),
        message: '업로드 작업이 등록되었습니다.',
        section: 'uploads'
      });
    } catch (error: any) {
      if (error?.response?.status === 413) {
        set({ message: '파일이 너무 큽니다. 최대 20MB까지 업로드할 수 있습니다. 지원 형식: pdf, txt, xlsx, csv, json, zip.' });
      } else {
        set({ message: getApiErrorMessage(error, 'PDF 업로드를 시작하지 못했습니다.') });
      }
    } finally {
      set({ uploading: false });
    }
  }
}));
