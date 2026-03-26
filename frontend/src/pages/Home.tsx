import { Link } from 'react-router-dom';
import { useEffect, useMemo, useState } from 'react';
import client from '../api/client';

type WordPage = {
  totalElements: number;
};

type TopWord = {
  rank: number;
  term: string;
  incorrectCount: number;
};

const featureRows = [
  { id: 'words', name: '경제 용어 검색', route: '/words', group: '학습', status: '용어 검색' },
  { id: 'quiz', name: '데일리 퀴즈', route: '/quiz', group: '학습', status: '문제 풀이' },
  { id: 'incorrect', name: '오답 노트', route: '/incorrect-note', group: '복습', status: '오답 관리' },
  { id: 'top', name: '오답 상위 단어', route: '/top-incorrect', group: '복습', status: '순위 보기' },
  { id: 'chat', name: 'AI 채팅', route: '/chat', group: 'AI', status: '대화 시작' },
  { id: 'recommend', name: 'AI 추천', route: '/ai-recommend', group: 'AI', status: '전략 보기' },
  { id: 'invest', name: 'AI 투자', route: '/ai-invest', group: 'AI', status: '분석 보기' },
  { id: 'stocks', name: '해외 주식', route: '/stocks', group: '투자', status: '차트 보기' },
  { id: 'krstocks', name: '국내 주식', route: '/kr-stocks', group: '투자', status: '시세 보기' }
] as const;

const contentTabs = ['학습', 'AI', '투자'] as const;
const filterChips = ['전체', '용어', '퀴즈', '오답', 'AI', '주식'] as const;
const sectionGroups: Record<(typeof contentTabs)[number], readonly string[]> = {
  학습: ['학습', '복습'],
  AI: ['AI'],
  투자: ['투자']
};

export default function Home() {
  const [wordCount, setWordCount] = useState<number | null>(null);
  const [topWords, setTopWords] = useState<TopWord[]>([]);
  const [activeSection, setActiveSection] = useState<(typeof contentTabs)[number]>('학습');
  const [activeFilter, setActiveFilter] = useState<(typeof filterChips)[number]>('전체');

  useEffect(() => {
    client.get<WordPage>('/words', { params: { page: 0, size: 1 } }).then((res) => setWordCount(res.data.totalElements)).catch(() => setWordCount(null));
    client.get<TopWord[]>('/quizzes/top-100').then((res) => setTopWords(res.data.slice(0, 8))).catch(() => setTopWords([]));
  }, []);

  const summaryCards = useMemo(
    () => [
      { title: '등록 용어', value: wordCount ? `${wordCount.toLocaleString()}개` : '-' },
      { title: '오늘의 퀴즈', value: 'Daily Quiz' },
      { title: 'AI 학습', value: 'Chat / Recommend / Invest' }
    ],
    [wordCount]
  );

  const filteredRows = useMemo(() => {
    const sectionMatched = featureRows.filter((item) => sectionGroups[activeSection].includes(item.group));

    if (activeFilter === '전체') {
      return sectionMatched;
    }

    return sectionMatched.filter((item) => {
      switch (activeFilter) {
        case '용어':
          return item.id === 'words';
        case '퀴즈':
          return item.id === 'quiz';
        case '오답':
          return item.id === 'incorrect' || item.id === 'top';
        case 'AI':
          return item.group === 'AI';
        case '주식':
          return item.group === '투자';
        default:
          return true;
      }
    });
  }, [activeFilter, activeSection]);

  const quickPanel = topWords.slice(0, 6);

  return (
    <div className="site-frame page-stack toss-market-screen real-feature-home">
      <section className="toss-market-layout">
        <div className="toss-market-main">
          <section className="toss-market-summary">
            <div className="toss-market-bullets">
              <span>용어 학습</span>
              <span>퀴즈 복습</span>
              <span>AI 보조 학습</span>
            </div>

            <div className="toss-summary-strip real-summary-strip">
              <button type="button" className="toss-day-chip active">오늘</button>
              {summaryCards.map((item) => (
                <article key={item.title} className="toss-summary-card neutral">
                  <span>{item.title}</span>
                  <strong>{item.value}</strong>
                </article>
              ))}
            </div>
          </section>

          <section className="toss-market-section-tabs">
            {contentTabs.map((item) => (
              <button
                key={item}
                type="button"
                className={`toss-market-section-tab${item === activeSection ? ' active' : ''}`}
                onClick={() => {
                  setActiveSection(item);
                  setActiveFilter('전체');
                }}
              >
                {item}
              </button>
            ))}
          </section>

          <section className="toss-market-filters">
            {filterChips.map((item) => (
              <button
                key={item}
                type="button"
                className={`toss-market-filter${item === activeFilter ? ' active' : ''}`}
                onClick={() => setActiveFilter(item)}
              >
                {item}
              </button>
            ))}
          </section>

          <section className="toss-table-wrap">
            <div className="toss-table-head">
              <span>기능</span>
              <span>구분</span>
              <span>바로가기</span>
            </div>

            <div className="toss-table-list">
              {filteredRows.map((item, index) => (
                <Link key={item.id} to={item.route} className="toss-table-row real-feature-row">
                  <div className="toss-table-rank">
                    <span className="toss-heart">•</span>
                    <strong>{index + 1}</strong>
                    <div className="toss-symbol-badge">{item.name.slice(0, 1)}</div>
                    <em>{item.name}</em>
                  </div>
                  <span className="toss-table-price">{item.group}</span>
                  <span className="toss-table-change up">{item.status}</span>
                </Link>
              ))}
              {filteredRows.length === 0 && (
                <div className="toss-table-row real-feature-row empty">
                  <div className="toss-table-rank">
                    <span className="toss-heart">•</span>
                    <strong>-</strong>
                    <div className="toss-symbol-badge">-</div>
                    <em>선택한 조건에 맞는 기능이 없습니다.</em>
                  </div>
                  <span className="toss-table-price">-</span>
                  <span className="toss-table-change">-</span>
                </div>
              )}
            </div>
          </section>
        </div>

        <aside className="toss-watch-sidebar">
          <div className="toss-watch-topbar">
            <h2>빠른 이동</h2>
          </div>

          <Link to="/ai-recommend" className="toss-watch-ai-banner">
            <span>AI 추천</span>
            <strong>투자 전략 대화 시작</strong>
          </Link>

          <div className="toss-watch-heading">
            <strong>최근 오답 상위</strong>
            <span>실제 퀴즈 집계 데이터</span>
          </div>

          <div className="toss-watch-list">
            {quickPanel.length === 0 && (
              <div className="toss-watch-item">
                <div className="toss-symbol-badge">-</div>
                <div className="toss-watch-copy">
                  <strong>오답 데이터 없음</strong>
                  <span>퀴즈를 풀면 집계됩니다.</span>
                </div>
              </div>
            )}
            {quickPanel.map((item) => (
              <Link key={`${item.rank}-${item.term}`} to="/top-incorrect" className="toss-watch-item">
                <div className="toss-symbol-badge">{item.term.slice(0, 1)}</div>
                <div className="toss-watch-copy">
                  <strong>{item.term}</strong>
                  <span>{item.rank}위</span>
                </div>
                <em className="up">{item.incorrectCount}회</em>
              </Link>
            ))}
          </div>

          <Link to="/top-incorrect" className="toss-watch-add">
            + 오답 순위 보기
          </Link>
        </aside>
      </section>
    </div>
  );
}
