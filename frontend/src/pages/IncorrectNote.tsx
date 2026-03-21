import { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { useQuizStore } from '../stores/quizStore';

export default function IncorrectNote() {
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const incorrectQuestions = useQuizStore((state) => state.incorrectQuestions);
  const initialize = useQuizStore((state) => state.initialize);

  useEffect(() => {
    initialize(isAuthenticated);
  }, [isAuthenticated, initialize]);

  const handleReview = (questionId: number) => {
    navigate('/quiz', { state: { reviewQuestionId: questionId } });
  };

  return (
    <div className="site-frame page-stack">
      <section className="panel">
        <div className="panel-head">
          <div>
            <p className="section-label">Incorrect Note</p>
            <h1>내 오답 노트</h1>
            <p className="panel-copy">첫 시도에 틀린 문항만 남겨두고, 필요할 때 다시 풀 수 있게 구성했습니다.</p>
          </div>
          <div className="button-row">
            <Link to="/quiz" className="button button-secondary">Daily Quiz</Link>
            <Link to="/top-incorrect" className="button button-secondary">오답 상위 단어</Link>
          </div>
        </div>
      </section>

      {!isAuthenticated && (
        <section className="panel callout-panel">
          <h2>로그인이 필요합니다.</h2>
          <p className="panel-copy">오답노트는 사용자별 첫 시도 오답 기록을 기준으로 표시됩니다.</p>
          <Link to="/signin" className="button button-primary">Sign In</Link>
        </section>
      )}

      {isAuthenticated && (
        <section className="panel">
          <div className="stack-list">
            {incorrectQuestions.length === 0 && <p className="muted">기록된 오답이 없습니다.</p>}
            {incorrectQuestions.map((item) => (
              <div key={item.questionId} className="list-row-block quiz-note-item">
                <div className="quiz-note-copy">
                  <strong>{item.questionText}</strong>
                  <p className="muted">{item.quizTitle}</p>
                  <p>{item.options.map((option) => option.optionText).join(' / ')}</p>
                </div>
                <button type="button" className="button button-primary" onClick={() => handleReview(item.questionId)}>
                  다시 풀기
                </button>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
