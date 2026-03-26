import { FormEvent, KeyboardEvent, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import AdvisorTranscript from '../components/AdvisorTranscript';
import InvestFeatureTabs from '../components/InvestFeatureTabs';
import { useAuthStore } from '../stores/authStore';
import { useAiInvestStore } from '../stores/aiInvestStore';
import { AiInvestMessage } from '../components/aiInvestTypes';

const RISK_PROFILES = ['공격적', '균형형', '안정적'];
const TRADE_STYLES = ['초단타', '단타', '스윙', '장투'];
const MARKETS = ['국내', '해외'];

export default function AIInvest() {
  const composerRef = useRef<HTMLTextAreaElement | null>(null);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  const threads = useAiInvestStore((s) => s.threads);
  const activeThread = useAiInvestStore((s) => s.activeThread);
  const stockName = useAiInvestStore((s) => s.stockName);
  const market = useAiInvestStore((s) => s.market);
  const riskProfile = useAiInvestStore((s) => s.riskProfile);
  const tradeStyle = useAiInvestStore((s) => s.tradeStyle);
  const notes = useAiInvestStore((s) => s.notes);
  const draft = useAiInvestStore((s) => s.draft);
  const loading = useAiInvestStore((s) => s.loading);
  const sidebarLoading = useAiInvestStore((s) => s.sidebarLoading);
  const error = useAiInvestStore((s) => s.error);

  const setStockName = useAiInvestStore((s) => s.setStockName);
  const setMarket = useAiInvestStore((s) => s.setMarket);
  const setRiskProfile = useAiInvestStore((s) => s.setRiskProfile);
  const setTradeStyle = useAiInvestStore((s) => s.setTradeStyle);
  const setNotes = useAiInvestStore((s) => s.setNotes);
  const setDraft = useAiInvestStore((s) => s.setDraft);
  const beginNewThread = useAiInvestStore((s) => s.beginNewThread);
  const reset = useAiInvestStore((s) => s.reset);
  const loadThreads = useAiInvestStore((s) => s.loadThreads);
  const selectThread = useAiInvestStore((s) => s.selectThread);
  const startThread = useAiInvestStore((s) => s.startThread);
  const removeThread = useAiInvestStore((s) => s.removeThread);
  const sendMessage = useAiInvestStore((s) => s.sendMessage);

  useEffect(() => {
    if (!isAuthenticated) { reset(); return; }
    beginNewThread();
    loadThreads(true);
  }, [isAuthenticated, beginNewThread, loadThreads, reset]);

  // Auto-resize textarea
  useEffect(() => {
    const el = composerRef.current;
    if (!el) return;
    el.style.height = '0px';
    el.style.height = `${Math.max(el.scrollHeight, 80)}px`;
  }, [draft]);

  const handleStart = async (e: FormEvent) => {
    e.preventDefault();
    await startThread();
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    await sendMessage();
  };

  const handleKeyDown = async (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key !== 'Enter' || e.metaKey || e.ctrlKey) return;
    e.preventDefault();
    if (!loading && activeThread) await sendMessage();
  };

  const toggleSidebar = () => {
    setSidebarOpen((current) => !current);
  };

  if (!isAuthenticated) {
    return (
      <div className="site-frame page-stack">
        <InvestFeatureTabs />

        <section className="panel callout-panel">
          <p className="section-label">AI Invest</p>
          <h1>DB금융투자 기반 투자 전략 대화</h1>
          <p className="panel-copy">
            국내·해외 종목명을 입력하면 DB금융투자 OPEN API 실시간 데이터와 AI 웹 검색을 결합해 투자 전략을 분석합니다.
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
      <InvestFeatureTabs />

      <section className={`panel chat-shell advisor-chat-shell advisor-history-shell ${sidebarOpen ? 'sidebar-open' : 'sidebar-closed'}`}>
        <div className="advisor-sidebar-rail">
          <button
            type="button"
            className={`advisor-history-toggle${sidebarOpen ? ' active' : ''}`}
            onClick={toggleSidebar}
            aria-label={sidebarOpen ? 'AI Invest 대화 기록 닫기' : 'AI Invest 대화 기록 열기'}
            aria-expanded={sidebarOpen}
          >
            <span className="advisor-history-toggle-icon" aria-hidden="true">
              <span className="advisor-history-toggle-rail" />
              <span className="advisor-history-toggle-lines">
                <span />
                <span />
              </span>
            </span>
          </button>
        </div>

        {sidebarOpen && (
          <aside className="chat-sidebar advisor-history-sidebar">
            <div className="chat-sidebar-head">
              <div>
                <p className="section-label">Investment Threads</p>
                <h2>AI Invest</h2>
              </div>
              <button
                type="button"
                className="button button-primary"
                onClick={beginNewThread}
                disabled={loading}
              >
                New Invest
              </button>
            </div>
            <div className="chat-thread-list">
              {sidebarLoading && <p className="muted">목록을 불러오는 중입니다.</p>}
              {!sidebarLoading && threads.length === 0 && (
                <p className="muted">생성된 투자 대화가 없습니다.</p>
              )}
              {threads.map((t) => (
                <div
                  key={t.threadId}
                  className={`chat-thread-item ${activeThread?.threadId === t.threadId ? 'active' : ''}`}
                >
                  <button
                    type="button"
                    className="chat-thread-link"
                    onClick={() => selectThread(t.threadId)}
                  >
                    <strong>{t.title}</strong>
                    <span>{t.stockName} · {t.market}</span>
                    <span>{t.riskProfile} · {t.tradeStyle}</span>
                  </button>
                  <button
                    type="button"
                    className="chat-thread-delete"
                    onClick={() => removeThread(t.threadId)}
                    aria-label="Delete invest thread"
                  >
                    Delete
                  </button>
                </div>
              ))}
            </div>
          </aside>
        )}

        <div className="chat-main advisor-chat-main">
          <div className="chat-main-head">
            <div>
              <p className="section-label">DB금융투자 OPEN API · 투자 전략 세션</p>
              <h2>{activeThread?.title ?? '새 투자 분석을 시작하세요'}</h2>
            </div>
          </div>

          <div className="advisor-workspace">
            {/* ── Session Setup ── */}
            <div className="advisor-context-stack">
              <div className="advisor-session-panel">
                <div className="advisor-session-head">
                  <div>
                    <p className="section-label">Session Setup</p>
                    <h3>종목과 투자 조건</h3>
                  </div>
                  <p className="muted advisor-session-copy">
                    국내 종목 코드(6자리) 입력 시 DB금융투자 실시간 데이터를 자동으로 불러와 분석에 활용합니다.
                  </p>
                </div>

                <form className="form-stack" onSubmit={handleStart}>
                  {/* Stock Name */}
                  <div className="stock-advisor-field">
                    <span>종목명 / 코드</span>
                    <input
                      value={stockName}
                      onChange={(e) => setStockName(e.target.value)}
                      placeholder="예: 삼성전자, 005930, NVIDIA, NVDA"
                    />
                  </div>

                  {/* Market */}
                  <div className="stock-advisor-field">
                    <span>시장</span>
                    <div className="stock-advisor-select-row">
                      {MARKETS.map((m) => (
                        <button
                          key={m}
                          type="button"
                          className={`button ${market === m ? 'button-primary' : 'button-secondary'}`}
                          onClick={() => setMarket(m)}
                        >
                          {m === '국내' ? '🇰🇷 국내' : '🌐 해외'}
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Risk + Style */}
                  <div className="stock-advisor-select-row">
                    <div className="stock-advisor-field">
                      <span>투자 성향</span>
                      <select value={riskProfile} onChange={(e) => setRiskProfile(e.target.value)}>
                        {RISK_PROFILES.map((r) => (
                          <option key={r} value={r}>{r}</option>
                        ))}
                      </select>
                    </div>
                    <div className="stock-advisor-field">
                      <span>매매 스타일</span>
                      <select value={tradeStyle} onChange={(e) => setTradeStyle(e.target.value)}>
                        {TRADE_STYLES.map((s) => (
                          <option key={s} value={s}>{s}</option>
                        ))}
                      </select>
                    </div>
                  </div>

                  {/* Notes */}
                  <div className="stock-advisor-field">
                    <span>투자 메모 (선택)</span>
                    <textarea
                      rows={3}
                      value={notes}
                      onChange={(e) => setNotes(e.target.value)}
                      placeholder="예: 현재 보유 중, 손절선 고민, 분할매수 계획 등"
                      maxLength={500}
                    />
                  </div>

                  <button
                    type="submit"
                    className="button button-primary"
                    disabled={loading || !stockName.trim()}
                  >
                    {loading ? '분석 중...' : activeThread ? '새 분석 시작' : '분석 시작'}
                  </button>
                </form>
              </div>

              {/* Info panel for domestic stocks */}
              {market === '국내' && (
                <div className="advisor-session-panel">
                  <p className="section-label">DB금융투자 OPEN API</p>
                  <p className="muted" style={{ fontSize: '13px', lineHeight: 1.6 }}>
                    국내 종목 코드(6자리, 예: 005930)를 입력하면 실시간 현재가, 1년 고점·저점, 지지·저항선을 자동으로 분석에 포함합니다.<br />
                    종목명 입력 시에는 웹 검색 기반으로 분석합니다.
                  </p>
                </div>
              )}
            </div>

            {/* ── Conversation ── */}
            <div className="advisor-conversation-shell">
              <section className="advisor-transcript-panel">
                {!activeThread ? (
                  <div className="chat-empty-state advisor-empty-state">
                    <p>종목명 또는 코드와 투자 조건을 입력한 뒤 분석 시작을 누르면 DB금융투자 데이터 기반 첫 브리핑이 생성됩니다.</p>
                    <p className="muted">이후 매수·매도 타이밍, 목표가, 손절선 등 추가 질문을 이어갈 수 있습니다.</p>
                  </div>
                ) : (
                  <AdvisorTranscript
                    threadKey={activeThread.threadId}
                    messages={activeThread.messages as AiInvestMessage[]}
                    loading={loading}
                    pendingTitle="AI Invest"
                    pendingText="DB금융투자 데이터와 시장 정보를 정리해서 투자 전략을 분석하고 있습니다."
                  />
                )}
              </section>

              <form className="chat-composer advisor-chat-composer" onSubmit={handleSubmit}>
                <textarea
                  ref={composerRef}
                  rows={1}
                  value={draft}
                  onChange={(e) => setDraft(e.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder={
                    activeThread
                      ? '예: 지금 매수해도 되는지, 목표가는 어디까지 볼지, 손절선은 어디로 잡을지 질문하세요.'
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
                  <button
                    type="submit"
                    className="button button-primary"
                    disabled={!activeThread || loading}
                  >
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
