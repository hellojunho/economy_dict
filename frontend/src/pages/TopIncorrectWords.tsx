import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { useQuizStore } from '../stores/quizStore';

export default function TopIncorrectWords() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const topWords = useQuizStore((state) => state.topWords);
  const initialize = useQuizStore((state) => state.initialize);

  useEffect(() => {
    initialize(isAuthenticated);
  }, [isAuthenticated, initialize]);

  return (
    <div className="site-frame page-stack">
      <section className="panel">
        <div className="panel-head">
          <div>
            <p className="section-label">Top Incorrect</p>
            <h1>오답 상위 단어</h1>
            <p className="panel-copy">전체 퀴즈 기록에서 오답 빈도가 높은 경제 용어를 순위로 정리했습니다.</p>
          </div>
          <div className="button-row">
            <Link to="/quiz" className="button button-secondary">Daily Quiz</Link>
            <Link to="/incorrect-note" className="button button-secondary">오답노트</Link>
          </div>
        </div>
      </section>

      <section className="panel">
        <div className="rank-list">
          {topWords.length === 0 && <p className="muted">아직 집계된 데이터가 없습니다.</p>}
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
      </section>
    </div>
  );
}
