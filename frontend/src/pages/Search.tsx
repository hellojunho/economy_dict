import { FormEvent, useEffect } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { useSearchStore } from '../stores/searchStore';

function formatMeaningMarkdown(content: string) {
  return content
    .replace(/\r\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}

function previewMeaning(content: string) {
  if (!content) {
    return '';
  }

  const line = content
    .split('\n')
    .map((item) => item.trim())
    .find((item) => item.length > 0 && !item.startsWith('**'));

  return (line ?? content).replace(/^"+|"+$/g, '');
}

export default function Search() {
  const query = useSearchStore((state) => state.query);
  const lookupResult = useSearchStore((state) => state.lookupResult);
  const listResponse = useSearchStore((state) => state.listResponse);
  const selectedWord = useSearchStore((state) => state.selectedWord);
  const loading = useSearchStore((state) => state.loading);
  const message = useSearchStore((state) => state.message);
  const page = useSearchStore((state) => state.page);
  const setQuery = useSearchStore((state) => state.setQuery);
  const selectWord = useSearchStore((state) => state.selectWord);
  const initialize = useSearchStore((state) => state.initialize);
  const search = useSearchStore((state) => state.search);
  const changePage = useSearchStore((state) => state.changePage);

  useEffect(() => {
    initialize();
  }, [initialize]);

  const handleSearch = async (event?: FormEvent) => {
    event?.preventDefault();
    await search();
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
                onClick={() => selectWord(item)}
              >
                <div>
                  <strong>{item.word}</strong>
                  <p>{previewMeaning(item.meaning)}</p>
                </div>
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
                <div className="definition-markdown">
                  <ReactMarkdown remarkPlugins={[remarkGfm]}>
                    {formatMeaningMarkdown(lookupResult.meaning)}
                  </ReactMarkdown>
                </div>
                <dl className="meta-list">
                  <div>
                    <dt>English</dt>
                    <dd>{lookupResult.englishWord || '-'}</dd>
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
                <div className="definition-markdown">
                  <ReactMarkdown remarkPlugins={[remarkGfm]}>
                    {formatMeaningMarkdown(selectedWord.meaning)}
                  </ReactMarkdown>
                </div>
                <dl className="meta-list">
                  <div>
                    <dt>English</dt>
                    <dd>{selectedWord.englishWord || '-'}</dd>
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
