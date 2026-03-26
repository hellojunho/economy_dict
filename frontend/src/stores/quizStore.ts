import { create } from 'zustand';
import client from '../api/client';
import { getApiErrorMessage } from '../utils/apiError';

export type DailyQuizOption = {
  optionId: number;
  optionText: string;
};

export type DailyQuizQuestion = {
  questionId: number;
  questionText: string;
  options: DailyQuizOption[];
};

export type DailyQuizResponse = {
  quizId: string;
  title: string;
  questions: DailyQuizQuestion[];
  solvedQuestionIds: number[];
  recordedCorrectCount: number;
  recordedIncorrectCount: number;
};

export type QuizResult = {
  totalQuestions: number;
  correctCount: number;
  completed: boolean;
  submittedCorrect: boolean;
  recordedCorrectCount: number;
  recordedIncorrectCount: number;
};

export type QuizFeedback = 'idle' | 'correct' | 'incorrect';

export type IncorrectQuizQuestion = {
  questionId: number;
  quizId: string;
  quizTitle: string;
  questionText: string;
  options: DailyQuizOption[];
};

export type TopWord = {
  rank: number;
  wordId: number;
  term: string;
  incorrectCount: number;
  definition: string;
};

export type QuizSessionMode = 'official' | 'retryAll' | 'retryIncorrect' | 'noteReview';

export type SessionQuestion = {
  questionId: number;
  quizId: string;
  quizTitle: string;
  questionText: string;
  options: DailyQuizOption[];
};

type QuizState = {
  dailyQuiz: DailyQuizResponse | null;
  answers: Record<number, number>;
  solvedQuestionIds: number[];
  sessionMode: QuizSessionMode;
  sessionQuestions: SessionQuestion[];
  currentQuestionIndex: number;
  sessionCompleted: boolean;
  feedback: QuizFeedback;
  incorrectQuestions: IncorrectQuizQuestion[];
  topWords: TopWord[];
  result: QuizResult | null;
  recordedCorrectCount: number;
  recordedIncorrectCount: number;
  message: string;
  loading: boolean;
  initialize: (isAuthenticated: boolean) => Promise<void>;
  selectAnswer: (questionId: number, optionId: number) => void;
  submitCurrent: () => Promise<void>;
  moveToNextQuestion: () => void;
  startRetryAll: () => void;
  startRetryIncorrect: () => void;
  startReviewQuestion: (questionId: number) => void;
  resumeOfficial: () => void;
  reset: () => void;
};

function toSessionQuestions(dailyQuiz: DailyQuizResponse): SessionQuestion[] {
  return dailyQuiz.questions.map((question) => ({
    questionId: question.questionId,
    quizId: dailyQuiz.quizId,
    quizTitle: dailyQuiz.title,
    questionText: question.questionText,
    options: question.options
  }));
}

function buildOfficialSession(dailyQuiz: DailyQuizResponse, solvedQuestionIds: number[]) {
  return toSessionQuestions(dailyQuiz).filter((question) => !solvedQuestionIds.includes(question.questionId));
}

function buildSessionState(sessionMode: QuizSessionMode, sessionQuestions: SessionQuestion[], message = '') {
  return {
    sessionMode,
    sessionQuestions,
    currentQuestionIndex: 0,
    sessionCompleted: sessionQuestions.length === 0,
    feedback: 'idle' as QuizFeedback,
    message
  };
}

async function fetchTopWords() {
  const response = await client.get<TopWord[]>('/quizzes/top-100');
  return response.data.slice(0, 10);
}

async function fetchDailyQuiz() {
  const response = await client.get<DailyQuizResponse>('/quizzes/daily');
  if (response.status === 204 || !response.data) {
    return null;
  }
  return response.data;
}

async function fetchIncorrectQuestions() {
  const response = await client.get<IncorrectQuizQuestion[]>('/quizzes/incorrect');
  return response.data;
}

export const useQuizStore = create<QuizState>((set, get) => ({
  dailyQuiz: null,
  answers: {},
  solvedQuestionIds: [],
  sessionMode: 'official',
  sessionQuestions: [],
  currentQuestionIndex: 0,
  sessionCompleted: false,
  feedback: 'idle',
  incorrectQuestions: [],
  topWords: [],
  result: null,
  recordedCorrectCount: 0,
  recordedIncorrectCount: 0,
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
      set({
        dailyQuiz: null,
        incorrectQuestions: [],
        answers: {},
        solvedQuestionIds: [],
        sessionMode: 'official',
        sessionQuestions: [],
        currentQuestionIndex: 0,
        sessionCompleted: false,
        feedback: 'idle',
        result: null,
        recordedCorrectCount: 0,
        recordedIncorrectCount: 0
      });
      return;
    }

    try {
      const [dailyQuiz, incorrectQuestions] = await Promise.all([fetchDailyQuiz(), fetchIncorrectQuestions()]);
      if (!dailyQuiz) {
        set({
          dailyQuiz: null,
          incorrectQuestions,
          answers: {},
          solvedQuestionIds: [],
          result: null,
          recordedCorrectCount: 0,
          recordedIncorrectCount: 0,
          ...buildSessionState('official', []),
          message: '아직 생성된 데일리 퀴즈가 없습니다. Admin에서 Create Quiz를 실행하세요.'
        });
        return;
      }
      set({
        dailyQuiz,
        incorrectQuestions,
        answers: {},
        solvedQuestionIds: dailyQuiz.solvedQuestionIds,
        result: null,
        recordedCorrectCount: dailyQuiz.recordedCorrectCount,
        recordedIncorrectCount: dailyQuiz.recordedIncorrectCount,
        ...buildSessionState('official', buildOfficialSession(dailyQuiz, dailyQuiz.solvedQuestionIds))
      });
    } catch (error) {
      set({
        dailyQuiz: null,
        incorrectQuestions: [],
        solvedQuestionIds: [],
        sessionQuestions: [],
        currentQuestionIndex: 0,
        sessionCompleted: false,
        feedback: 'idle',
        recordedCorrectCount: 0,
        recordedIncorrectCount: 0,
        message: getApiErrorMessage(error, '데일리 퀴즈를 불러오지 못했습니다.')
      });
    }
  },
  selectAnswer: (questionId, optionId) => set((state) => ({
    answers: { ...state.answers, [questionId]: optionId },
    feedback: 'idle',
    message: ''
  })),
  submitCurrent: async () => {
    const {
      dailyQuiz,
      answers,
      currentQuestionIndex,
      sessionQuestions,
      solvedQuestionIds
    } = get();
    const currentQuestion = sessionQuestions[currentQuestionIndex];
    if (!dailyQuiz || !currentQuestion) {
      return;
    }

    const selectedOptionId = answers[currentQuestion.questionId];
    if (!selectedOptionId) {
      set({ message: '보기를 선택한 뒤 정답을 제출하세요.', feedback: 'idle' });
      return;
    }

    set({ loading: true, message: '' });
    try {
      const response = await client.post<QuizResult>(`/quizzes/${currentQuestion.quizId}/submit`, {
        answers: [{
          questionId: currentQuestion.questionId,
          selectedOptionId
        }]
      });
      const [incorrectQuestions, topWords] = await Promise.all([fetchIncorrectQuestions(), fetchTopWords()]);
      const isDailyQuizQuestion = currentQuestion.quizId === dailyQuiz.quizId;
      const nextSolvedQuestionIds = response.data.submittedCorrect && isDailyQuizQuestion && !solvedQuestionIds.includes(currentQuestion.questionId)
        ? [...solvedQuestionIds, currentQuestion.questionId]
        : solvedQuestionIds;

      set({
        result: response.data,
        incorrectQuestions,
        topWords,
        solvedQuestionIds: nextSolvedQuestionIds,
        recordedCorrectCount: isDailyQuizQuestion ? response.data.recordedCorrectCount : get().recordedCorrectCount,
        recordedIncorrectCount: isDailyQuizQuestion ? response.data.recordedIncorrectCount : get().recordedIncorrectCount,
        feedback: response.data.submittedCorrect ? 'correct' : 'incorrect',
        message: response.data.submittedCorrect ? '정답입니다.' : '틀렸습니다. 다시 시도하세요.'
      });
    } catch (error) {
      set({ feedback: 'idle', message: getApiErrorMessage(error, '퀴즈 제출에 실패했습니다. 다시 시도하세요.') });
    } finally {
      set({ loading: false });
    }
  },
  moveToNextQuestion: () => {
    const { currentQuestionIndex, sessionMode, sessionQuestions } = get();
    const nextIndex = currentQuestionIndex + 1;
    if (nextIndex >= sessionQuestions.length) {
      set({
        sessionCompleted: true,
        feedback: 'idle',
        message: sessionMode === 'official' ? '모든 문항을 풀었습니다.' : '다시풀기를 마쳤습니다.'
      });
      return;
    }
    set({
      currentQuestionIndex: nextIndex,
      feedback: 'idle',
      message: ''
    });
  },
  startRetryAll: () => {
    const { dailyQuiz } = get();
    if (!dailyQuiz) {
      return;
    }
    set({
      answers: {},
      result: null,
      ...buildSessionState('retryAll', toSessionQuestions(dailyQuiz))
    });
  },
  startRetryIncorrect: () => {
    const { dailyQuiz, incorrectQuestions } = get();
    if (!dailyQuiz) {
      return;
    }
    const retryQuestions = incorrectQuestions.filter((question) => question.quizId === dailyQuiz.quizId);
    if (retryQuestions.length === 0) {
      set({ message: '현재 퀴즈에서 기록된 오답이 없습니다.' });
      return;
    }
    set({
      answers: {},
      result: null,
      ...buildSessionState('retryIncorrect', retryQuestions)
    });
  },
  startReviewQuestion: (questionId) => {
    const question = get().incorrectQuestions.find((item) => item.questionId === questionId);
    if (!question) {
      set({ message: '오답 문항을 찾지 못했습니다.' });
      return;
    }
    set({
      answers: {},
      result: null,
      ...buildSessionState('noteReview', [question], '오답노트 문항을 다시 풉니다.')
    });
  },
  resumeOfficial: () => {
    const { dailyQuiz, solvedQuestionIds } = get();
    if (!dailyQuiz) {
      return;
    }
    set({
      answers: {},
      result: null,
      ...buildSessionState('official', buildOfficialSession(dailyQuiz, solvedQuestionIds))
    });
  },
  reset: () => set({
    dailyQuiz: null,
    answers: {},
    solvedQuestionIds: [],
    sessionMode: 'official',
    sessionQuestions: [],
    currentQuestionIndex: 0,
    sessionCompleted: false,
    feedback: 'idle',
    incorrectQuestions: [],
    topWords: [],
    result: null,
    recordedCorrectCount: 0,
    recordedIncorrectCount: 0,
    message: '',
    loading: false
  })
}));
