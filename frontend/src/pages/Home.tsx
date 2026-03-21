import { Link } from 'react-router-dom';
import { useEffect, useState } from 'react';
import client from '../api/client';

type WordPage = {
  totalElements: number;
};

type TopWord = {
  rank: number;
  term: string;
  incorrectCount: number;
};

export default function Home() {
  const [wordCount, setWordCount] = useState<number | null>(null);
  const [topWords, setTopWords] = useState<TopWord[]>([]);

  useEffect(() => {
    client.get<WordPage>('/words', { params: { page: 0, size: 1 } }).then((res) => setWordCount(res.data.totalElements)).catch(() => setWordCount(null));
    client.get<TopWord[]>('/quizzes/top-100').then((res) => setTopWords(res.data.slice(0, 5))).catch(() => setTopWords([]));
  }, []);

  return (
    <div className="site-frame page-stack">
      <section className="hero-panel">
        <div>
          <p className="section-label">Formal Learning Workspace</p>
          <h1 className="hero-title">경제 용어 학습과 퀴즈 운영을 하나의 체계로 정리한 플랫폼</h1>
          <p className="hero-copy">
            문서 기반 단어 적재, 용어 검색, 데일리 퀴즈, 오답 관리, 관리자 운영 화면까지 동일한 데이터 구조 위에서 동작합니다.
          </p>
          <div className="hero-actions">
            <Link to="/words" className="button button-primary">용어 검색</Link>
            <Link to="/quiz" className="button button-secondary">데일리 퀴즈</Link>
          </div>
        </div>
        <div className="stat-grid">
          <article className="stat-card">
            <span>Indexed Words</span>
            <strong>{wordCount ?? '-'}</strong>
            <p>DB에 누적된 경제 용어 수</p>
          </article>
          <article className="stat-card">
            <span>AI Import</span>
            <strong>Batch</strong>
            <p>대용량 PDF를 비동기 Batch로 적재</p>
          </article>
          <article className="stat-card">
            <span>Quiz Flow</span>
            <strong>Daily</strong>
            <p>오답 누적과 상위 오답 분석 제공</p>
          </article>
        </div>
      </section>

      <section className="content-grid columns-2-1">
        <article className="panel">
          <div className="panel-head">
            <div>
              <p className="section-label">Platform Scope</p>
              <h2>핵심 기능</h2>
            </div>
          </div>
          <div className="feature-list">
            <div>
              <strong>Words</strong>
              <p>경제 용어 검색, 목록 탐색, 세부 의미 확인, AI 기반 미등록 용어 보강</p>
            </div>
            <div>
              <strong>Quiz</strong>
              <p>데일리 퀴즈 응시, 오답 노트, 상위 오답 단어 순위 제공</p>
            </div>
            <div>
              <strong>Admin</strong>
              <p>사용자/단어 CRUD, 업로드 작업 상태 추적, 일간 지표 확인</p>
            </div>
          </div>
        </article>

        <aside className="panel">
          <div className="panel-head compact">
            <div>
              <p className="section-label">Top Incorrect</p>
              <h2>최근 오답 상위</h2>
            </div>
          </div>
          <div className="rank-list">
            {topWords.length === 0 && <p className="muted">아직 집계된 데이터가 없습니다.</p>}
            {topWords.map((item) => (
              <div key={`${item.rank}-${item.term}`} className="rank-item">
                <span>{item.rank}</span>
                <strong>{item.term}</strong>
                <em>{item.incorrectCount}회</em>
              </div>
            ))}
          </div>
        </aside>
      </section>
    </div>
  );
}
