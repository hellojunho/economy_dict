import { create } from 'zustand';
import client from '../api/client';
import { getApiErrorMessage } from '../utils/apiError';

export type DailyQuizQuestion = {
  wordId: number;
  term: string;
  options: string[];
};

export type DailyQuizResponse = {
  questions: DailyQuizQuestion[];
};

export type QuizResult = {
  totalCount: number;
  correctCount: number;
};

export type IncorrectWord = {
  wordId: number;
  term: string;
  definition: string;
};

export type TopWord = {
  rank: number;
  wordId: number;
  term: string;
  incorrectCount: number;
  definition: string;
};

type QuizState = {
  dailyQuiz: DailyQuizResponse | null;
  answers: Record<number, string>;
  incorrectWords: IncorrectWord[];
  topWords: TopWord[];
  result: QuizResult | null;
  message: string;
  loading: boolean;
  initialize: (isAuthenticated: boolean) => Promise<void>;
  selectAnswer: (wordId: number, answer: string) => void;
  submit: () => Promise<void>;
  reset: () => void;
};

async function fetchTopWords() {
  const response = await client.get<TopWord[]>('/quizzes/top-100');
  return response.data.slice(0, 10);
}

async function fetchDailyQuiz() {
  const response = await client.get<DailyQuizResponse>('/quizzes/daily');
  return response.data;
}

async function fetchIncorrectWords() {
  const response = await client.get<IncorrectWord[]>('/quizzes/incorrect');
  return response.data;
}

export const useQuizStore = create<QuizState>((set, get) => ({
  dailyQuiz: null,
  answers: {},
  incorrectWords: [],
  topWords: [],
  result: null,
  message: '',
  loading: false,
  initialize: async (isAuthenticated) => {
    set({ message: '' });
    try {
      const topWords = await fetchTopWords();
      set({ topWords });
    } catch {
      set({ topWords: [] });
    }

    if (!isAuthenticated) {
      set({ dailyQuiz: null, incorrectWords: [], answers: {}, result: null });
      return;
    }

    try {
      const [dailyQuiz, incorrectWords] = await Promise.all([fetchDailyQuiz(), fetchIncorrectWords()]);
      set({ dailyQuiz, incorrectWords, answers: {}, result: null });
    } catch (error) {
      set({ message: getApiErrorMessage(error, '데일리 퀴즈를 불러오지 못했습니다.') });
    }
  },
  selectAnswer: (wordId, answer) => set((state) => ({ answers: { ...state.answers, [wordId]: answer } })),
  submit: async () => {
    const { dailyQuiz, answers } = get();
    if (!dailyQuiz) {
      return;
    }

    set({ loading: true, message: '' });
    try {
      const payload = {
        answers: dailyQuiz.questions.map((question) => ({
          wordId: question.wordId,
          selectedAnswer: answers[question.wordId] ?? ''
        }))
      };
      const response = await client.post<QuizResult>('/quizzes/submit', payload);
      const [incorrectWords, topWords] = await Promise.all([fetchIncorrectWords(), fetchTopWords()]);
      set({ result: response.data, incorrectWords, topWords });
    } catch (error) {
      set({ message: getApiErrorMessage(error, '퀴즈 제출에 실패했습니다. 모든 문항을 확인하세요.') });
    } finally {
      set({ loading: false });
    }
  },
  reset: () => set({
    dailyQuiz: null,
    answers: {},
    incorrectWords: [],
    topWords: [],
    result: null,
    message: '',
    loading: false
  })
}));
