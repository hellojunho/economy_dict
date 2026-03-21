import { FormEvent, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { useQuizStore } from '../stores/quizStore';

export default function Quiz() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const dailyQuiz = useQuizStore((state) => state.dailyQuiz);
  const answers = useQuizStore((state) => state.answers);
  const incorrectWords = useQuizStore((state) => state.incorrectWords);
  const topWords = useQuizStore((state) => state.topWords);
  const result = useQuizStore((state) => state.result);
  const message = useQuizStore((state) => state.message);
  const loading = useQuizStore((state) => state.loading);
  const initialize = useQuizStore((state) => state.initialize);
  const selectAnswer = useQuizStore((state) => state.selectAnswer);
  const submit = useQuizStore((state) => state.submit);

  useEffect(() => {
    initialize(isAuthenticated);
  }, [isAuthenticated, initialize]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    await submit();
  };

  const completion = useMemo(() => {
    const total = dailyQuiz?.questions.length ?? 0;
    if (!total) {
      return 0;
    }
    const answered = (dailyQuiz?.questions ?? []).filter((question) => answers[question.wordId]).length;
    return Math.round((answered * 100) / total);
  }, [answers, dailyQuiz]);

  return (
    <div className="site-frame page-stack">
      <section className="panel quiz-hero">
        <div>
          <p className="section-label">Daily Quiz</p>
          <h1>오늘의 경제 용어 퀴즈</h1>
          <p className="panel-copy">로그인한 사용자에게 10문항 데일리 퀴즈를 제공하고, 결과는 오답 노트와 상위 오답 집계에 반영됩니다.</p>
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
              <p className="section-label">Quiz Form</p>
              <h2>문항 응시</h2>
            </div>
            <button type="submit" className="button button-primary" disabled={loading}>
              {loading ? 'Submitting...' : '정답 제출'}
            </button>
          </div>

          <div className="quiz-question-list">
            {dailyQuiz.questions.map((question, index) => (
              <article key={question.wordId} className="question-card">
                <div className="question-head">
                  <span>{String(index + 1).padStart(2, '0')}</span>
                  <strong>{question.term}</strong>
                </div>
                <div className="option-grid">
                  {question.options.map((option) => (
                    <label key={option} className={`option-card ${answers[question.wordId] === option ? 'selected' : ''}`}>
                      <input
                        type="radio"
                        name={`word-${question.wordId}`}
                        checked={answers[question.wordId] === option}
                        onChange={() => selectAnswer(question.wordId, option)}
                      />
                      <span>{option}</span>
                    </label>
                  ))}
                </div>
              </article>
            ))}
          </div>

          {result && (
            <div className="result-banner">
              정답 {result.correctCount} / {result.totalCount}
            </div>
          )}
          {message && <p className="form-message error-text">{message}</p>}
        </form>
      )}

      <section className="content-grid columns-2-1">
        <article className="panel">
          <div className="panel-head compact">
            <div>
              <p className="section-label">Incorrect Note</p>
              <h2>내 오답 노트</h2>
            </div>
          </div>
          <div className="stack-list">
            {isAuthenticated && incorrectWords.length === 0 && <p className="muted">기록된 오답이 없습니다.</p>}
            {!isAuthenticated && <p className="muted">로그인 후 개인 오답 노트를 확인할 수 있습니다.</p>}
            {incorrectWords.map((item) => (
              <div key={item.wordId} className="list-row-block">
                <strong>{item.term}</strong>
                <p>{item.definition}</p>
              </div>
            ))}
          </div>
        </article>

        <aside className="panel">
          <div className="panel-head compact">
            <div>
              <p className="section-label">Top 100</p>
              <h2>오답 상위 단어</h2>
            </div>
          </div>
          <div className="rank-list">
            {topWords.map((item) => (
              <div key={item.wordId} className="rank-item expanded">
                <span>{item.rank}</span>
                <div>
                  <strong>{item.term}</strong>
                  <p>{item.definition}</p>
                </div>
                <em>{item.incorrectCount}회</em>
              </div>
            ))}
          </div>
        </aside>
      </section>
    </div>
  );
}
