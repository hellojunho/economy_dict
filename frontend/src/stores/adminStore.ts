import { create } from 'zustand';
import client from '../api/client';
import { getApiErrorMessage } from '../utils/apiError';

export type SectionKey = 'overview' | 'users' | 'words' | 'uploads' | 'quizzes';

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
  fileType?: string | null;
  sourceId?: number | null;
  sourceName?: string | null;
};

export type AdminWordPage = {
  content: AdminWord[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type SourceOption = {
  id: number;
  name: string;
};

export type FileTypeOption = {
  code: string;
  displayName: string;
};

export type UploadTask = {
  fileId: string;
  originalFileName?: string | null;
  status: string;
  message: string;
  estimatedTime?: string | null;
  progressPercent?: number | null;
  errorLog?: string | null;
};

export type UploadAiModelConfig = {
  currentModel: string;
  defaultModel: string;
};

const DEFAULT_UPLOAD_MODEL = 'gpt-4.1-nano';

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

export type AdminQuizOption = {
  id: number;
  questionId: number;
  optionText: string;
  optionOrder: number;
  correct: boolean;
  selectedCount: number;
};

export type AdminQuizQuestion = {
  id: number;
  quizId: number;
  questionText: string;
  attemptedUsers: number;
  correctUsers: number;
  correctRate: number;
  participants: string[];
  correctParticipants: string[];
  options: AdminQuizOption[];
};

export type AdminQuiz = {
  id: number;
  quizId: string;
  title: string;
  questionCount: number;
  participantCount: number;
  createdAt?: string | null;
  questions: AdminQuizQuestion[];
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
  fileType: string;
  sourceId: string;
  sourceName: string;
};

type AdminState = {
  section: SectionKey;
  summary: Summary | null;
  stats: DailyStat[];
  users: AdminUser[];
  words: AdminWord[];
  quizzes: AdminQuiz[];
  selectedQuiz: AdminQuiz | null;
  wordListResponse: AdminWordPage | null;
  wordPage: number;
  uploads: UploadTask[];
  sourceOptions: SourceOption[];
  fileTypeOptions: FileTypeOption[];
  message: string;
  loading: boolean;
  uploading: boolean;
  applyingUploadModel: boolean;
  translatingWords: boolean;
  generatingQuiz: boolean;
  selectedFile: File | null;
  uploadSourceId: string;
  uploadSourceName: string;
  uploadModel: string;
  uploadModelDraft: string;
  userForm: UserFormState;
  wordForm: WordFormState;
  setSection: (section: SectionKey) => void;
  setSelectedFile: (file: File | null) => void;
  setUploadSourceId: (sourceId: string) => void;
  setUploadSourceName: (sourceName: string) => void;
  setUploadModelDraft: (model: string) => void;
  updateUserForm: (patch: Partial<UserFormState>) => void;
  updateWordForm: (patch: Partial<WordFormState>) => void;
  editUser: (user: AdminUser) => void;
  editWord: (word: AdminWord) => void;
  resetUserForm: () => void;
  resetWordForm: () => void;
  clearSelectedQuiz: () => void;
  clearMessage: () => void;
  refreshCurrentSection: (sectionOverride?: SectionKey) => Promise<void>;
  changeWordPage: (page: number) => Promise<void>;
  loadUploads: () => Promise<void>;
  applyUploadModel: () => Promise<void>;
  saveUser: () => Promise<boolean>;
  deleteUser: (id: number) => Promise<void>;
  loadWord: (id: number) => Promise<void>;
  saveWord: () => Promise<boolean>;
  deleteWord: (id: number) => Promise<void>;
  translateWordsToEnglish: () => Promise<void>;
  uploadSelectedFile: () => Promise<void>;
  selectQuiz: (id: number) => Promise<void>;
  generateQuiz: () => Promise<AdminQuiz | null>;
};

const WORDS_PAGE_SIZE = 10;

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
  fileType: 'MANUAL',
  sourceId: '',
  sourceName: ''
};

export const DIRECT_SOURCE_OPTION = '__direct__';

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

async function fetchWords(page: number) {
  const response = await client.get<AdminWordPage>('/admin/words', {
    params: {
      page,
      size: WORDS_PAGE_SIZE
    }
  });
  return response.data;
}

async function fetchWord(id: number) {
  const response = await client.get<AdminWord>(`/admin/words/${id}`);
  return response.data;
}

async function fetchUploads() {
  const response = await client.get<UploadTask[]>('/admin/words/uploads');
  return response.data;
}

async function fetchUploadAiModelConfig() {
  const response = await client.get<UploadAiModelConfig>('/admin/openai/upload-model');
  return response.data;
}

async function fetchQuizzes() {
  const response = await client.get<AdminQuiz[]>('/admin/quizzes');
  return response.data;
}

async function fetchQuiz(id: number) {
  const response = await client.get<AdminQuiz>(`/admin/quizzes/${id}`);
  return response.data;
}

async function fetchSources() {
  const response = await client.get<SourceOption[]>('/admin/sources');
  return response.data;
}

async function fetchFileTypes() {
  const response = await client.get<FileTypeOption[]>('/admin/file-types');
  return response.data;
}

function buildWordPayload(wordForm: WordFormState) {
  return {
    word: wordForm.word,
    meaning: wordForm.meaning,
    englishWord: wordForm.englishWord || null,
    englishMeaning: wordForm.englishMeaning || null,
    fileType: wordForm.fileType || 'MANUAL',
    sourceId: wordForm.sourceId && wordForm.sourceId !== DIRECT_SOURCE_OPTION ? Number(wordForm.sourceId) : null,
    sourceName: wordForm.sourceId === DIRECT_SOURCE_OPTION ? wordForm.sourceName : null
  };
}

async function fetchWordSection(page: number) {
  const [wordListResponse, sourceOptions, fileTypeOptions] = await Promise.all([
    fetchWords(page),
    fetchSources(),
    fetchFileTypes()
  ]);

  return { wordListResponse, sourceOptions, fileTypeOptions };
}

export const useAdminStore = create<AdminState>((set, get) => ({
  section: 'overview',
  summary: null,
  stats: [],
  users: [],
  words: [],
  quizzes: [],
  selectedQuiz: null,
  wordListResponse: null,
  wordPage: 0,
  uploads: [],
  sourceOptions: [],
  fileTypeOptions: [],
  message: '',
  loading: false,
  uploading: false,
  applyingUploadModel: false,
  translatingWords: false,
  generatingQuiz: false,
  selectedFile: null,
  uploadSourceId: '',
  uploadSourceName: '',
  uploadModel: DEFAULT_UPLOAD_MODEL,
  uploadModelDraft: DEFAULT_UPLOAD_MODEL,
  userForm: defaultUserForm,
  wordForm: defaultWordForm,
  setSection: (section) => set({ section }),
  setSelectedFile: (file) => set({ selectedFile: file }),
  setUploadSourceId: (sourceId) => set({ uploadSourceId: sourceId, uploadSourceName: sourceId === DIRECT_SOURCE_OPTION ? get().uploadSourceName : '' }),
  setUploadSourceName: (sourceName) => set({ uploadSourceName: sourceName }),
  setUploadModelDraft: (model) => set({ uploadModelDraft: model }),
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
      fileType: word.fileType ?? 'MANUAL',
      sourceId: word.sourceId ? String(word.sourceId) : '',
      sourceName: word.sourceId ? '' : (word.sourceName ?? '')
    }
  }),
  resetUserForm: () => set({ userForm: defaultUserForm }),
  resetWordForm: () => set({ wordForm: defaultWordForm }),
  clearSelectedQuiz: () => set({ selectedQuiz: null }),
  clearMessage: () => set({ message: '' }),
  refreshCurrentSection: async (sectionOverride) => {
    const section = sectionOverride ?? get().section;
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
        const { wordPage } = get();
        const { wordListResponse, sourceOptions, fileTypeOptions } = await fetchWordSection(wordPage);
        set({
          words: wordListResponse.content,
          wordListResponse,
          wordPage: wordListResponse.page,
          sourceOptions,
          fileTypeOptions
        });
      }
      if (section === 'uploads') {
        const [uploads, sourceOptions] = await Promise.all([
          fetchUploads(),
          fetchSources()
        ]);
        let resolvedModel = DEFAULT_UPLOAD_MODEL;
        try {
          const uploadAiModelConfig = await fetchUploadAiModelConfig();
          resolvedModel = uploadAiModelConfig.currentModel || uploadAiModelConfig.defaultModel || DEFAULT_UPLOAD_MODEL;
        } catch {
          resolvedModel = get().uploadModel || get().uploadModelDraft || DEFAULT_UPLOAD_MODEL;
        }
        set({
          uploads,
          sourceOptions,
          uploadModel: resolvedModel,
          uploadModelDraft: resolvedModel
        });
      }
      if (section === 'quizzes') {
        const quizzes = await fetchQuizzes();
        set({ quizzes, selectedQuiz: null });
      }
    } catch (error) {
      set({ message: getApiErrorMessage(error, '관리자 데이터를 불러오지 못했습니다.') });
    } finally {
      set({ loading: false });
    }
  },
  changeWordPage: async (page) => {
    set({ loading: true, message: '' });
    try {
      const wordListResponse = await fetchWords(page);
      set({
        words: wordListResponse.content,
        wordListResponse,
        wordPage: wordListResponse.page
      });
    } catch (error) {
      set({ message: getApiErrorMessage(error, '단어 페이지를 불러오지 못했습니다.') });
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
  applyUploadModel: async () => {
    const { uploadModelDraft } = get();
    set({ applyingUploadModel: true, message: '' });
    try {
      const response = await client.put<UploadAiModelConfig>('/admin/openai/upload-model', {
        model: uploadModelDraft
      });
      set({
        uploadModel: response.data.currentModel,
        uploadModelDraft: response.data.currentModel,
        message: `업로드 GPT 모델이 ${response.data.currentModel}로 적용되었습니다.`
      });
    } catch (error) {
      set({ message: getApiErrorMessage(error, '업로드 GPT 모델을 적용하지 못했습니다.') });
    } finally {
      set({ applyingUploadModel: false });
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
      return true;
    } catch (error) {
      set({ message: getApiErrorMessage(error, '사용자 정보를 저장하지 못했습니다.') });
      return false;
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
  loadWord: async (id) => {
    set({ loading: true, message: '' });
    try {
      const [word, sourceOptions, fileTypeOptions] = await Promise.all([
        fetchWord(id),
        fetchSources(),
        fetchFileTypes()
      ]);
      set({ sourceOptions, fileTypeOptions });
      get().editWord(word);
    } catch (error) {
      set({ message: getApiErrorMessage(error, '단어 정보를 불러오지 못했습니다.') });
    } finally {
      set({ loading: false });
    }
  },
  saveWord: async () => {
    const { wordForm } = get();
    set({ message: '' });
    try {
      const targetPage = wordForm.id ? get().wordPage : 0;
      const payload = buildWordPayload(wordForm);
      if (wordForm.id) {
        await client.put(`/admin/words/${wordForm.id}`, payload);
      } else {
        await client.post('/admin/words', payload);
      }
      const { wordListResponse, sourceOptions, fileTypeOptions } = await fetchWordSection(targetPage);
      set({
        wordForm: defaultWordForm,
        words: wordListResponse.content,
        wordListResponse,
        wordPage: wordListResponse.page,
        sourceOptions,
        fileTypeOptions,
        message: '단어 정보가 저장되었습니다.'
      });
      return true;
    } catch (error) {
      set({ message: getApiErrorMessage(error, '단어 정보를 저장하지 못했습니다.') });
      return false;
    }
  },
  deleteWord: async (id) => {
    try {
      await client.delete(`/admin/words/${id}`);
      const currentPage = get().wordPage;
      let wordListResponse = await fetchWords(currentPage);
      if (!wordListResponse.content.length && currentPage > 0) {
        wordListResponse = await fetchWords(currentPage - 1);
      }
      set((state) => ({
        words: wordListResponse.content,
        wordListResponse,
        wordPage: wordListResponse.page,
        wordForm: state.wordForm.id === id ? defaultWordForm : state.wordForm
      }));
    } catch (error) {
      set({ message: getApiErrorMessage(error, '단어 삭제에 실패했습니다.') });
    }
  },
  translateWordsToEnglish: async () => {
    set({ translatingWords: true, message: '' });
    try {
      const response = await client.post<UploadTask>('/admin/words/to-english');
      const uploads = await fetchUploads();
      set({
        uploads,
        message: response.data.message
      });
    } catch (error) {
      set({ message: getApiErrorMessage(error, '영문화 작업에 실패했습니다.') });
    } finally {
      set({ translatingWords: false });
    }
  },
  uploadSelectedFile: async () => {
    const { selectedFile, uploadSourceId, uploadSourceName } = get();
    if (!selectedFile) {
      set({ message: '업로드할 파일을 선택하세요. 지원 형식: pdf, txt, xlsx, csv, json, zip. 최대 20MB까지 허용됩니다.' });
      return;
    }

    set({ uploading: true, message: '' });
    try {
      const formData = new FormData();
      formData.append('file', selectedFile);
      if (uploadSourceId && uploadSourceId !== DIRECT_SOURCE_OPTION) {
        formData.append('sourceId', uploadSourceId);
      }
      if (uploadSourceId === DIRECT_SOURCE_OPTION && uploadSourceName.trim()) {
        formData.append('sourceName', uploadSourceName.trim());
      }
      await client.post('/admin/words/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      const [uploads, sourceOptions] = await Promise.all([fetchUploads(), fetchSources()]);
      set({
        selectedFile: null,
        uploadSourceId: '',
        uploadSourceName: '',
        uploads,
        sourceOptions,
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
  },
  selectQuiz: async (id) => {
    set({ loading: true, message: '' });
    try {
      set({ selectedQuiz: await fetchQuiz(id) });
    } catch (error) {
      set({ message: getApiErrorMessage(error, '퀴즈 상세 정보를 불러오지 못했습니다.') });
    } finally {
      set({ loading: false });
    }
  },
  generateQuiz: async () => {
    set({ generatingQuiz: true, message: '' });
    try {
      const response = await client.post<AdminQuiz>('/admin/quizzes/generate');
      const quizzes = await fetchQuizzes();
      set({
        quizzes,
        selectedQuiz: response.data,
        section: 'quizzes',
        message: 'AI 퀴즈가 생성되었습니다.'
      });
      return response.data;
    } catch (error) {
      set({ message: getApiErrorMessage(error, 'AI 퀴즈 생성에 실패했습니다.') });
      return null;
    } finally {
      set({ generatingQuiz: false });
    }
  }
}));
