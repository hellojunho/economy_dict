import { FormEvent, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { useQuizStore } from '../stores/quizStore';

function getSessionHeading(sessionMode: string, currentQuestionIndex: number, totalQuestions: number) {
  if (sessionMode === 'retryAll') {
    return `전체 다시풀기 ${Math.min(currentQuestionIndex + 1, totalQuestions)} / ${totalQuestions}`;
  }
  if (sessionMode === 'retryIncorrect') {
    return `오답 다시풀기 ${Math.min(currentQuestionIndex + 1, totalQuestions)} / ${totalQuestions}`;
  }
  if (sessionMode === 'noteReview') {
    return `오답노트 다시풀기 ${Math.min(currentQuestionIndex + 1, totalQuestions)} / ${totalQuestions}`;
  }
  return `문항 ${Math.min(currentQuestionIndex + 1, totalQuestions)} / ${totalQuestions}`;
}

function getSessionLabel(sessionMode: string) {
  if (sessionMode === 'retryAll') {
    return 'Retry All';
  }
  if (sessionMode === 'retryIncorrect') {
    return 'Retry Incorrect';
  }
  if (sessionMode === 'noteReview') {
    return 'Incorrect Note';
  }
  return 'Quiz Form';
}

export default function Quiz() {
  const location = useLocation();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const dailyQuiz = useQuizStore((state) => state.dailyQuiz);
  const answers = useQuizStore((state) => state.answers);
  const solvedQuestionIds = useQuizStore((state) => state.solvedQuestionIds);
  const sessionMode = useQuizStore((state) => state.sessionMode);
  const sessionQuestions = useQuizStore((state) => state.sessionQuestions);
  const currentQuestionIndex = useQuizStore((state) => state.currentQuestionIndex);
  const sessionCompleted = useQuizStore((state) => state.sessionCompleted);
  const feedback = useQuizStore((state) => state.feedback);
  const incorrectQuestions = useQuizStore((state) => state.incorrectQuestions);
  const recordedCorrectCount = useQuizStore((state) => state.recordedCorrectCount);
  const recordedIncorrectCount = useQuizStore((state) => state.recordedIncorrectCount);
  const message = useQuizStore((state) => state.message);
  const loading = useQuizStore((state) => state.loading);
  const initialize = useQuizStore((state) => state.initialize);
  const selectAnswer = useQuizStore((state) => state.selectAnswer);
  const submitCurrent = useQuizStore((state) => state.submitCurrent);
  const moveToNextQuestion = useQuizStore((state) => state.moveToNextQuestion);
  const startRetryAll = useQuizStore((state) => state.startRetryAll);
  const startRetryIncorrect = useQuizStore((state) => state.startRetryIncorrect);
  const startReviewQuestion = useQuizStore((state) => state.startReviewQuestion);
  const resumeOfficial = useQuizStore((state) => state.resumeOfficial);

  useEffect(() => {
    let cancelled = false;
    const reviewQuestionId = (location.state as { reviewQuestionId?: number } | null)?.reviewQuestionId;
    initialize(isAuthenticated).then(() => {
      if (!cancelled && reviewQuestionId) {
        startReviewQuestion(reviewQuestionId);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [isAuthenticated, initialize, location.state, startReviewQuestion]);

  useEffect(() => {
    if (feedback !== 'correct') {
      return;
    }
    const timer = window.setTimeout(() => {
      moveToNextQuestion();
    }, 700);
    return () => window.clearTimeout(timer);
  }, [feedback, moveToNextQuestion]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    await submitCurrent();
  };

  const totalQuestions = dailyQuiz?.questions.length ?? 0;
  const completion = totalQuestions ? Math.round((solvedQuestionIds.length * 100) / totalQuestions) : 0;
  const officialCompleted = Boolean(dailyQuiz && solvedQuestionIds.length === dailyQuiz.questions.length);
  const activeQuestion = sessionQuestions[currentQuestionIndex] ?? null;
  const currentQuizIncorrectQuestions = dailyQuiz
    ? incorrectQuestions.filter((item) => item.quizId === dailyQuiz.quizId)
    : [];
  const showSummary = officialCompleted;

  return (
    <div className="site-frame page-stack">
      <section className="panel quiz-hero">
        <div>
          <p className="section-label">Daily Quiz</p>
          <h1>오늘의 경제 용어 퀴즈</h1>
          <p className="panel-copy">관리자가 생성한 최신 퀴즈를 Daily Quiz에서 응시하고, 완료 후에는 전체 문항과 오답 문항을 언제든 다시 풀 수 있습니다.</p>
          {dailyQuiz && <p className="muted">{dailyQuiz.title}</p>}
        </div>
        {isAuthenticated && (
          <div className="progress-card">
            <span>Completion</span>
            <strong>{completion}%</strong>
            <div className="progress-track large">
              <div className="progress-fill" style={{ width: `${completion}%` }} />
            </div>
          </div>
        )}
      </section>

      {!isAuthenticated && (
        <section className="panel callout-panel">
          <h2>로그인이 필요합니다.</h2>
          <p className="panel-copy">데일리 퀴즈와 오답 노트는 인증된 사용자 기준으로 기록됩니다.</p>
          <Link to="/signin" className="button button-primary">Sign In</Link>
        </section>
      )}

      {isAuthenticated && dailyQuiz && (
        <form className="panel quiz-form-panel" onSubmit={handleSubmit}>
          <div className="panel-head">
            <div>
              <p className="section-label">{getSessionLabel(sessionMode)}</p>
              <h2>
                {sessionCompleted
                  ? sessionMode === 'official'
                    ? '퀴즈 완료'
                    : '다시풀기 완료'
                  : getSessionHeading(sessionMode, currentQuestionIndex, sessionQuestions.length)}
              </h2>
            </div>
            <div className="button-row">
              {sessionMode !== 'official' && (
                <button type="button" className="button button-secondary" onClick={resumeOfficial}>
                  공식 풀이로 돌아가기
                </button>
              )}
              {!sessionCompleted && activeQuestion && (
                <button type="submit" className="button button-primary" disabled={loading}>
                  {loading ? 'Submitting...' : '정답 제출'}
                </button>
              )}
            </div>
          </div>

          {showSummary && (
            <div className="quiz-summary-block">
              <div className="quiz-summary-grid">
                <div className="quiz-summary-card">
                  <span>기록된 정답</span>
                  <strong>{recordedCorrectCount}문항</strong>
                  <p>첫 시도에 맞힌 문제 수입니다.</p>
                </div>
                <div className="quiz-summary-card summary-incorrect">
                  <span>기록된 오답</span>
                  <strong>{recordedIncorrectCount}문항</strong>
                  <p>첫 시도에 틀린 문제 수입니다.</p>
                </div>
              </div>
              <div className="button-row">
                <button type="button" className="button button-primary" onClick={startRetryAll}>
                  전체 다시풀기
                </button>
                <button type="button" className="button button-secondary" onClick={startRetryIncorrect}>
                  오답만 다시풀기
                </button>
              </div>
            </div>
          )}

          {activeQuestion && !sessionCompleted && (
            <div className="quiz-question-list">
              <article className={`question-card quiz-single-card ${feedback === 'correct' ? 'quiz-correct' : ''} ${feedback === 'incorrect' ? 'quiz-incorrect' : ''}`}>
                <div className="question-head">
                  <span>{String(currentQuestionIndex + 1).padStart(2, '0')}</span>
                  <div className="quiz-question-copy">
                    <strong>{activeQuestion.questionText}</strong>
                    {sessionMode !== 'official' && <p className="muted">{activeQuestion.quizTitle}</p>}
                  </div>
                </div>
                <div className="option-grid">
                  {activeQuestion.options.map((option) => (
                    <label key={option.optionId} className={`option-card ${answers[activeQuestion.questionId] === option.optionId ? 'selected' : ''}`}>
                      <input
                        type="radio"
                        name={`question-${activeQuestion.questionId}`}
                        checked={answers[activeQuestion.questionId] === option.optionId}
                        onChange={() => selectAnswer(activeQuestion.questionId, option.optionId)}
                      />
                      <span>{option.optionText}</span>
                    </label>
                  ))}
                </div>
              </article>
            </div>
          )}

          {sessionCompleted && !showSummary && (
            <div className="result-banner">
              다시풀기를 마쳤습니다. 다른 세션을 시작하거나 공식 풀이 화면으로 돌아갈 수 있습니다.
            </div>
          )}

          {message && <p className="form-message error-text">{message}</p>}
        </form>
      )}

      {isAuthenticated && !dailyQuiz && (
        <section className="panel callout-panel">
          <h2>표시할 퀴즈가 없습니다.</h2>
          <p className="panel-copy">{message || '아직 생성된 데일리 퀴즈가 없습니다. Admin에서 Create Quiz를 실행하세요.'}</p>
        </section>
      )}

      <section className="panel">
        <div className="panel-head compact">
          <div>
            <p className="section-label">Quiz Tools</p>
            <h2>학습 기록 보기</h2>
          </div>
        </div>
        <div className="button-row">
          <Link to="/incorrect-note" className="button button-secondary">오답노트 페이지</Link>
          <Link to="/top-incorrect" className="button button-secondary">오답 상위 단어 페이지</Link>
        </div>
        {showSummary && currentQuizIncorrectQuestions.length > 0 && (
          <div className="result-banner">
            현재 퀴즈에서 첫 시도에 틀린 문항 {currentQuizIncorrectQuestions.length}개가 오답노트에 기록되어 있습니다.
          </div>
        )}
      </section>
    </div>
  );
}
