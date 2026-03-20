import { FormEvent, useEffect, useState } from 'react';
import client from '../api/client';

type WordRecord = {
  id: number;
  word: string;
  meaning: string;
  englishWord?: string | null;
  englishMeaning?: string | null;
  source?: string | null;
};

type WordPage = {
  content: WordRecord[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export default function Search() {
  const [query, setQuery] = useState('');
  const [lookupResult, setLookupResult] = useState<WordRecord | null>(null);
  const [listResponse, setListResponse] = useState<WordPage | null>(null);
  const [selectedWord, setSelectedWord] = useState<WordRecord | null>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [page, setPage] = useState(0);

  const loadWords = async (nextQuery: string, nextPage: number) => {
    const response = await client.get<WordPage>('/words', {
      params: {
        q: nextQuery || undefined,
        page: nextPage,
        size: 12
      }
    });
    setListResponse(response.data);
    if (response.data.content.length > 0) {
      setSelectedWord((current) => current && response.data.content.some((item) => item.id === current.id)
        ? current
        : response.data.content[0]);
    } else {
      setSelectedWord(null);
    }
  };

  const lookupWord = async (term: string) => {
    if (!term.trim()) {
      setLookupResult(null);
      return;
    }
    const response = await client.get<WordRecord>('/words/lookup', { params: { q: term } });
    setLookupResult(response.data);
    setSelectedWord(response.data);
  };

  const handleSearch = async (event?: FormEvent) => {
    event?.preventDefault();
    setLoading(true);
    setMessage('');
    try {
      await Promise.all([loadWords(query, 0), lookupWord(query)]);
      setPage(0);
    } catch {
      setMessage('검색 요청을 처리하지 못했습니다. 잠시 후 다시 시도하세요.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadWords('', 0).catch(() => setMessage('용어 목록을 불러오지 못했습니다.'));
  }, []);

  const changePage = async (nextPage: number) => {
    setLoading(true);
    try {
      await loadWords(query, nextPage);
      setPage(nextPage);
    } catch {
      setMessage('페이지를 이동하지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="site-frame page-stack">
      <section className="panel search-hero">
        <div>
          <p className="section-label">Word Search</p>
          <h1>경제 용어 검색</h1>
          <p className="panel-copy">입력한 용어를 우선 조회하고, 미등록 용어는 AI 정의를 생성해 DB에 반영합니다.</p>
        </div>
        <form className="search-form" onSubmit={handleSearch}>
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="예: 주식, 물가상승률, 기준금리"
          />
          <button type="submit" className="button button-primary" disabled={loading}>
            {loading ? 'Searching...' : 'Search'}
          </button>
        </form>
        {message && <p className="form-message error-text">{message}</p>}
      </section>

      <section className="content-grid columns-2-1 words-layout">
        <article className="panel">
          <div className="panel-head compact">
            <div>
              <p className="section-label">Browse</p>
              <h2>용어 목록</h2>
            </div>
            <span className="data-chip">{listResponse?.totalElements ?? 0} Terms</span>
          </div>

          <div className="word-list">
            {listResponse?.content.map((item) => (
              <button
                key={item.id}
                type="button"
                className={`word-row ${selectedWord?.id === item.id ? 'selected' : ''}`}
                onClick={() => setSelectedWord(item)}
              >
                <div>
                  <strong>{item.word}</strong>
                  <p>{item.meaning}</p>
                </div>
                <span>{item.source ?? 'DB'}</span>
              </button>
            ))}
            {!listResponse?.content.length && <p className="muted">조건에 맞는 용어가 없습니다.</p>}
          </div>

          <div className="pager-row">
            <button
              type="button"
              className="button button-secondary"
              onClick={() => changePage(Math.max(page - 1, 0))}
              disabled={page === 0 || loading}
            >
              Previous
            </button>
            <span className="pager-meta">
              Page {(listResponse?.page ?? 0) + 1} / {Math.max(listResponse?.totalPages ?? 1, 1)}
            </span>
            <button
              type="button"
              className="button button-secondary"
              onClick={() => changePage(page + 1)}
              disabled={loading || !listResponse || page + 1 >= listResponse.totalPages}
            >
              Next
            </button>
          </div>
        </article>

        <aside className="detail-stack">
          <section className="panel emphasis-panel">
            <div className="panel-head compact">
              <div>
                <p className="section-label">Lookup</p>
                <h2>직접 검색 결과</h2>
              </div>
            </div>
            {lookupResult ? (
              <div className="definition-block">
                <h3>{lookupResult.word}</h3>
                <p>{lookupResult.meaning}</p>
                <dl className="meta-list">
                  <div>
                    <dt>English</dt>
                    <dd>{lookupResult.englishWord || '-'}</dd>
                  </div>
                  <div>
                    <dt>English Meaning</dt>
                    <dd>{lookupResult.englishMeaning || '-'}</dd>
                  </div>
                  <div>
                    <dt>Source</dt>
                    <dd>{lookupResult.source || 'DB'}</dd>
                  </div>
                </dl>
              </div>
            ) : (
              <p className="muted">검색어를 입력하면 즉시 정의를 확인할 수 있습니다.</p>
            )}
          </section>

          <section className="panel">
            <div className="panel-head compact">
              <div>
                <p className="section-label">Detail</p>
                <h2>선택된 용어</h2>
              </div>
            </div>
            {selectedWord ? (
              <div className="definition-block">
                <h3>{selectedWord.word}</h3>
                <p>{selectedWord.meaning}</p>
                <dl className="meta-list">
                  <div>
                    <dt>English</dt>
                    <dd>{selectedWord.englishWord || '-'}</dd>
                  </div>
                  <div>
                    <dt>English Meaning</dt>
                    <dd>{selectedWord.englishMeaning || '-'}</dd>
                  </div>
                  <div>
                    <dt>Source</dt>
                    <dd>{selectedWord.source || 'DB'}</dd>
                  </div>
                </dl>
              </div>
            ) : (
              <p className="muted">목록에서 용어를 선택하세요.</p>
            )}
          </section>
        </aside>
      </section>
    </div>
  );
}
