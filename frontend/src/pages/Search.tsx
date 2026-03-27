import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { useSearchStore } from '../stores/searchStore';

type DictionaryTab = 'search' | 'all';

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
  const listQuery = useSearchStore((state) => state.listQuery);
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
  const [activeTab, setActiveTab] = useState<DictionaryTab>('search');

  // 모달에 표시할 단어: 목록에서 클릭한 용어
  const modalWord = selectedWord;
  const normalizedQuery = query.trim();
  const hasCurrentSearchResult = normalizedQuery.length > 0 && listQuery === normalizedQuery;
  const relatedWords = useMemo(
    () =>
      hasCurrentSearchResult
        ? listResponse?.content.filter((item) => item.id !== lookupResult?.id) ?? []
        : [],
    [hasCurrentSearchResult, listResponse, lookupResult]
  );

  useEffect(() => {
    initialize();
  }, [initialize]);

  // ESC 키로 모달 닫기
  const closeModal = useCallback(() => {
    selectWord(null as never);
  }, [selectWord]);

  useEffect(() => {
    if (!modalWord) return;
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') closeModal();
    };
    window.addEventListener('keydown', handleKey);
    return () => window.removeEventListener('keydown', handleKey);
  }, [modalWord, closeModal]);

  const handleSearch = async (event?: FormEvent) => {
    event?.preventDefault();
    await search();
    setActiveTab('search');
  };

  const handleBrowseAll = async () => {
    setActiveTab('all');
    await initialize();
  };

  return (
    <div className="site-frame page-stack">
      <section className="panel search-hero dictionary-hero">
        <div className="dictionary-hero-copy">
          <p className="section-label">Economy Dictionary</p>
          <h1>경제사전</h1>
          <p className="panel-copy">
            검색으로 바로 찾거나, 전체 용어를 가나다 순으로 둘러볼 수 있습니다.
          </p>
        </div>
        <form className="search-form dictionary-search-form" onSubmit={handleSearch}>
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="예: 주식, 물가상승률, 기준금리"
            aria-label="경제 용어 검색"
          />
          <button type="submit" className="button button-primary" disabled={loading}>
            {loading && activeTab === 'search' ? '찾는 중...' : '찾기'}
          </button>
        </form>
        {message && <p className="form-message error-text">{message}</p>}
        <div className="dictionary-tab-row" role="tablist" aria-label="사전 탐색 탭">
          <button
            type="button"
            className={`dictionary-tab${activeTab === 'search' ? ' active' : ''}`}
            onClick={() => setActiveTab('search')}
            role="tab"
            aria-selected={activeTab === 'search'}
          >
            사전 검색
          </button>
          <button
            type="button"
            className={`dictionary-tab${activeTab === 'all' ? ' active' : ''}`}
            onClick={handleBrowseAll}
            role="tab"
            aria-selected={activeTab === 'all'}
          >
            전체 용어
          </button>
        </div>
      </section>

      {activeTab === 'search' ? (
        <article className="panel dictionary-panel">
          <div className="panel-head compact">
            <div>
              <p className="section-label">Search</p>
              <h2>검색 결과</h2>
            </div>
            {hasCurrentSearchResult && <span className="data-chip">{listResponse?.totalElements ?? 0}건</span>}
          </div>

          {!normalizedQuery && (
            <div className="dictionary-empty-state">
              <strong>찾고 싶은 경제 용어를 입력하세요.</strong>
              <p>예: 기준금리, 유동성, 경기침체</p>
            </div>
          )}

          {normalizedQuery && !hasCurrentSearchResult && (
            <div className="dictionary-empty-state">
              <strong>현재 검색어로 다시 찾아보세요.</strong>
              <p>검색어를 바꾸었거나 전체 용어 목록을 열어둔 상태입니다.</p>
            </div>
          )}

          {hasCurrentSearchResult && lookupResult && (
            <div className="dictionary-result-section">
              <p className="section-label">Best Match</p>
              <button
                type="button"
                className="word-row"
                onClick={() => selectWord(lookupResult)}
              >
                <div>
                  <strong>{lookupResult.word}</strong>
                  <p>{previewMeaning(lookupResult.meaning)}</p>
                </div>
                {lookupResult.englishWord && (
                  <span className="word-row-en">{lookupResult.englishWord}</span>
                )}
              </button>
            </div>
          )}

          {hasCurrentSearchResult && relatedWords.length > 0 && (
            <div className="dictionary-result-section">
              <p className="section-label">Related Terms</p>
              <div className="word-list">
                {relatedWords.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    className="word-row"
                    onClick={() => selectWord(item)}
                  >
                    <div>
                      <strong>{item.word}</strong>
                      <p>{previewMeaning(item.meaning)}</p>
                    </div>
                    {item.englishWord && <span className="word-row-en">{item.englishWord}</span>}
                  </button>
                ))}
              </div>
            </div>
          )}

          {hasCurrentSearchResult && !lookupResult && !relatedWords.length && (
            <p className="muted">검색 결과가 없습니다.</p>
          )}
        </article>
      ) : (
        <article className="panel dictionary-panel">
          <div className="panel-head compact">
            <div>
              <p className="section-label">Browse</p>
              <h2>전체 용어</h2>
            </div>
            <span className="data-chip">{listResponse?.totalElements ?? 0}개</span>
          </div>

          <div className="word-list">
            {listResponse?.content.map((item) => (
              <button
                key={item.id}
                type="button"
                className="word-row"
                onClick={() => selectWord(item)}
              >
                <div>
                  <strong>{item.word}</strong>
                  <p>{previewMeaning(item.meaning)}</p>
                </div>
                {item.englishWord && <span className="word-row-en">{item.englishWord}</span>}
              </button>
            ))}
            {!listResponse?.content.length && <p className="muted">등록된 용어가 없습니다.</p>}
          </div>

          <div className="pager-row">
            <button
              type="button"
              className="button button-secondary"
              onClick={() => changePage(Math.max(page - 1, 0))}
              disabled={page === 0 || loading}
            >
              이전
            </button>
            <span className="pager-meta">
              {(listResponse?.page ?? 0) + 1} / {Math.max(listResponse?.totalPages ?? 1, 1)} 페이지
            </span>
            <button
              type="button"
              className="button button-secondary"
              onClick={() => changePage(page + 1)}
              disabled={loading || !listResponse || page + 1 >= listResponse.totalPages}
            >
              다음
            </button>
          </div>
        </article>
      )}

      {modalWord && (
        <div className="word-modal-overlay" onClick={closeModal}>
          <div className="word-modal" onClick={(e) => e.stopPropagation()}>
            <div className="word-modal-header">
              <div>
                <h2 className="word-modal-title">{modalWord.word}</h2>
                {modalWord.englishWord && (
                  <p className="word-modal-en">{modalWord.englishWord}</p>
                )}
              </div>
              <button
                type="button"
                className="word-modal-close"
                onClick={closeModal}
                aria-label="닫기"
              >
                ✕
              </button>
            </div>
            <div className="word-modal-body definition-markdown">
              <ReactMarkdown remarkPlugins={[remarkGfm]}>
                {formatMeaningMarkdown(modalWord.meaning)}
              </ReactMarkdown>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
