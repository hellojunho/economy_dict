import { FormEvent, KeyboardEvent, useEffect, useRef } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import AdvisorChartSnapshot from '../components/AdvisorChartSnapshot';
import StockAdvisorControls from '../components/StockAdvisorControls';
import StockAdvisorConversation from '../components/StockAdvisorConversation';
import { TradingViewSymbolOption } from '../components/stockAdvisorTypes';
import { useAuthStore } from '../stores/authStore';
import { useStockAdvisorStore } from '../stores/stockAdvisorStore';

function toSelectedSymbol(symbol: string): TradingViewSymbolOption | null {
  const normalized = symbol.trim().toUpperCase();
  if (!normalized) {
    return null;
  }

  return {
    symbol: normalized,
    description: normalized,
    exchange: normalized.split(':')[0] ?? '',
    type: 'stock',
    country: '',
    directViewOnly: normalized.startsWith('KRX:'),
    directViewOnlyMessage: normalized.startsWith('KRX:')
      ? '이 심볼은 TradingView 임베드보다 TradingView 웹사이트에서 직접 조회해야 할 수 있습니다.'
      : ''
  };
}

export default function AIRecommend() {
  const composerRef = useRef<HTMLTextAreaElement | null>(null);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const threads = useStockAdvisorStore((state) => state.threads);
  const activeThread = useStockAdvisorStore((state) => state.activeThread);
  const symbol = useStockAdvisorStore((state) => state.symbol);
  const riskProfile = useStockAdvisorStore((state) => state.riskProfile);
  const tradeStyle = useStockAdvisorStore((state) => state.tradeStyle);
  const notes = useStockAdvisorStore((state) => state.notes);
  const draft = useStockAdvisorStore((state) => state.draft);
  const loading = useStockAdvisorStore((state) => state.loading);
  const sidebarLoading = useStockAdvisorStore((state) => state.sidebarLoading);
  const error = useStockAdvisorStore((state) => state.error);
  const setSymbol = useStockAdvisorStore((state) => state.setSymbol);
  const setRiskProfile = useStockAdvisorStore((state) => state.setRiskProfile);
  const setTradeStyle = useStockAdvisorStore((state) => state.setTradeStyle);
  const setNotes = useStockAdvisorStore((state) => state.setNotes);
  const setDraft = useStockAdvisorStore((state) => state.setDraft);
  const beginNewThread = useStockAdvisorStore((state) => state.beginNewThread);
  const reset = useStockAdvisorStore((state) => state.reset);
  const loadThreads = useStockAdvisorStore((state) => state.loadThreads);
  const selectThread = useStockAdvisorStore((state) => state.selectThread);
  const startThread = useStockAdvisorStore((state) => state.startThread);
  const removeThread = useStockAdvisorStore((state) => state.removeThread);
  const sendMessage = useStockAdvisorStore((state) => state.sendMessage);
  const [searchParams] = useSearchParams();
  const symbolParam = searchParams.get('symbol')?.trim().toUpperCase() ?? '';

  useEffect(() => {
    if (!isAuthenticated) {
      reset();
      return;
    }

    beginNewThread(symbolParam || undefined);
    loadThreads(!symbolParam);
  }, [beginNewThread, isAuthenticated, loadThreads, reset, symbolParam]);

  useEffect(() => {
    const composer = composerRef.current;
    if (!composer) {
      return;
    }

    composer.style.height = '0px';
    composer.style.height = `${Math.max(composer.scrollHeight, 80)}px`;
  }, [draft]);

  const handleStart = async (event: FormEvent) => {
    event.preventDefault();
    await startThread();
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    await sendMessage();
  };

  const handleComposerKeyDown = async (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key !== 'Enter') {
      return;
    }

    if (event.metaKey || event.ctrlKey) {
      return;
    }

    event.preventDefault();
    if (!loading && activeThread) {
      await sendMessage();
    }
  };

  if (!isAuthenticated) {
    return (
      <div className="site-frame page-stack">
        <section className="panel callout-panel">
          <p className="section-label">AI Recommend</p>
          <h1>투자 전략 대화</h1>
          <p className="panel-copy">
            종목, 투자 성향, 매매 스타일을 설정한 뒤 AI와 대화를 이어가며 가격대와 전략을 점검할 수 있습니다.
          </p>
          <div className="button-row">
            <Link to="/signin" className="button button-primary">Sign In</Link>
            <Link to="/signup" className="button button-secondary">Create Account</Link>
          </div>
        </section>
      </div>
    );
  }

  return (
    <div className="site-frame page-stack">
      <section className="panel chat-shell advisor-chat-shell">
        <aside className="chat-sidebar">
          <div className="chat-sidebar-head">
            <div>
              <p className="section-label">Investment Threads</p>
              <h2>AI Recommend</h2>
            </div>
            <button
              type="button"
              className="button button-primary"
              onClick={() => beginNewThread(symbolParam || symbol || undefined)}
              disabled={loading}
            >
              New Recommend
            </button>
          </div>
          <div className="chat-thread-list">
            {sidebarLoading && <p className="muted">목록을 불러오는 중입니다.</p>}
            {!sidebarLoading && threads.length === 0 && <p className="muted">생성된 추천 대화가 없습니다.</p>}
            {threads.map((thread) => (
              <div
                key={thread.threadId}
                className={`chat-thread-item ${activeThread?.threadId === thread.threadId ? 'active' : ''}`}
              >
                <button type="button" className="chat-thread-link" onClick={() => selectThread(thread.threadId)}>
                  <strong>{thread.title}</strong>
                  <span>{thread.symbol}</span>
                  <span>{thread.riskProfile} · {thread.tradeStyle}</span>
                </button>
                <button
                  type="button"
                  className="chat-thread-delete"
                  onClick={() => removeThread(thread.threadId)}
                  aria-label="Delete recommendation thread"
                >
                  Delete
                </button>
              </div>
            ))}
          </div>
        </aside>

        <div className="chat-main advisor-chat-main">
          <div className="chat-main-head">
            <div>
              <p className="section-label">Stock Strategy Session</p>
              <h2>{activeThread?.title ?? '새 추천을 시작하세요'}</h2>
            </div>
          </div>

          <div className="advisor-workspace">
            <div className="advisor-context-stack">
              <div className="advisor-session-panel">
                <div className="advisor-session-head">
                  <div>
                    <p className="section-label">Session Setup</p>
                    <h3>종목과 투자 조건</h3>
                  </div>
                  <p className="muted advisor-session-copy">
                    입력한 설정을 기준으로 현재가, 1년 고점·저점, 지지·저항과 스타일별 전략을 대화형으로 이어갑니다.
                  </p>
                </div>

                <StockAdvisorControls
                  symbol={symbol}
                  selectedSymbol={toSelectedSymbol(symbol)}
                  riskProfile={riskProfile}
                  tradeStyle={tradeStyle}
                  notes={notes}
                  loading={loading}
                  onRiskProfileChange={setRiskProfile}
                  onTradeStyleChange={setTradeStyle}
                  onNotesChange={setNotes}
                  onSymbolSelect={(option) => setSymbol(option.symbol)}
                  onSymbolInputChange={setSymbol}
                  onSubmit={handleStart}
                  submitLabel={activeThread ? '새 분석 시작' : '분석 시작'}
                />
              </div>

              <AdvisorChartSnapshot symbol={symbol} tradeStyle={tradeStyle} />
            </div>

            <div className="advisor-conversation-shell">
              <section className="advisor-transcript-panel">
                {!activeThread && (
                  <div className="chat-empty-state advisor-empty-state">
                    <p>상단에서 종목과 투자 조건을 정한 뒤 `분석 시작`을 누르면 첫 전략 브리핑이 생성됩니다.</p>
                    <p className="muted">이후에는 아래 입력창에서 매수/매도, 목표가, 손절가, 지지선 확인 등 추가 질문을 이어갈 수 있습니다.</p>
                  </div>
                )}

                {activeThread && (
                  <StockAdvisorConversation messages={activeThread.messages} loading={loading} />
                )}
              </section>

              <form className="chat-composer advisor-chat-composer" onSubmit={handleSubmit}>
                <textarea
                  ref={composerRef}
                  rows={1}
                  value={draft}
                  onChange={(event) => setDraft(event.target.value)}
                  onKeyDown={handleComposerKeyDown}
                  placeholder={
                    activeThread
                      ? '예: 지금 구간에서 분할매수해도 되는지, 지지선 이탈 시 대응은 어떻게 볼지 질문하세요.'
                      : '먼저 상단 설정으로 분석을 시작하세요.'
                  }
                  disabled={!activeThread || loading}
                  className="chat-composer-textarea advisor-chat-composer-textarea"
                />
                <div className="button-row chat-composer-actions">
                  <p className="chat-composer-hint">
                    {activeThread
                      ? '`Enter` 전송, `Cmd+Enter` 또는 `Ctrl+Enter` 줄바꿈'
                      : '새 분석을 시작한 뒤 대화를 이어갈 수 있습니다.'}
                  </p>
                  <button type="submit" className="button button-primary" disabled={!activeThread || loading}>
                    {loading ? 'Sending...' : 'Send'}
                  </button>
                </div>
              </form>
            </div>
          </div>

          {error && <p className="form-message error-text">{error}</p>}
        </div>
      </section>
    </div>
  );
}
