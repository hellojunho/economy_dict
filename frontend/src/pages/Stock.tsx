import { Link } from 'react-router-dom';
import { useState } from 'react';
import InvestFeatureTabs from '../components/InvestFeatureTabs';
import StockAdvisorWidget from '../components/StockAdvisorWidget';
import StockMarketWarning from '../components/StockMarketWarning';
import TradingViewAdvancedChart from '../components/TradingViewAdvancedChart';
import { useThemeStore } from '../stores/themeStore';

const DEFAULT_SYMBOL = 'NASDAQ:TSLA';
const stockPresets = [
  { symbol: 'NASDAQ:TSLA', label: 'Tesla', summary: '전기차 대표주' },
  { symbol: 'NASDAQ:NVDA', label: 'NVIDIA', summary: 'AI 반도체 대표주' },
  { symbol: 'NASDAQ:AAPL', label: 'Apple', summary: '대형 기술주' },
  { symbol: 'NASDAQ:MSFT', label: 'Microsoft', summary: '클라우드/AI' },
  { symbol: 'AMEX:SPY', label: 'S&P 500 ETF', summary: '대표 지수 ETF' }
] as const;

export default function Stock() {
  const [activeSymbol, setActiveSymbol] = useState(DEFAULT_SYMBOL);
  const theme = useThemeStore((state) => state.theme);
  const activePreset = stockPresets.find((item) => item.symbol === activeSymbol) ?? stockPresets[0];

  return (
    <div className="site-frame page-stack stock-page">
      <InvestFeatureTabs recommendHref={`/ai-recommend?symbol=${encodeURIComponent(activeSymbol)}`} />

      <section className="panel toss-stock-hero">
        <div>
          <p className="section-label">Global Market</p>
          <h1>해외 주식 홈</h1>
          <div className="stock-hero-actions">
            <Link to={`/ai-recommend?symbol=${encodeURIComponent(activeSymbol)}`} className="button button-primary">AI 추천</Link>
            <Link to="/kr-stocks" className="button button-secondary">국내 주식</Link>
          </div>
        </div>
        <div className="toss-stock-hero-card">
          <span>Now Watching</span>
          <strong>{activePreset.label}</strong>
          <p>{activePreset.summary}</p>
        </div>
      </section>

      <section className="stock-filter-strip">
        <button type="button" className="stock-filter-chip active">전체</button>
        <button type="button" className="stock-filter-chip">차트</button>
        <Link to={`/ai-recommend?symbol=${encodeURIComponent(activeSymbol)}`} className="stock-filter-chip">AI 추천</Link>
        <Link to="/ai-invest" className="stock-filter-chip">AI 투자</Link>
        <Link to="/kr-stocks" className="stock-filter-chip">국내 주식</Link>
      </section>

      <section className="toss-stock-preset-strip">
        {stockPresets.map((preset) => (
          <button
            key={preset.symbol}
            type="button"
            className={`toss-stock-preset ${preset.symbol === activeSymbol ? 'active' : ''}`}
            onClick={() => setActiveSymbol(preset.symbol)}
          >
            <strong>{preset.label}</strong>
            <span>{preset.summary}</span>
          </button>
        ))}
      </section>

      <section className="content-grid columns-2-1 stock-summary-layout">
        <article className="panel stock-tv-panel toss-market-panel">
          <div className="stock-chart-tabs">
            <button type="button" className="stock-chart-tab active">실시간 차트</button>
            <Link to={`/ai-recommend?symbol=${encodeURIComponent(activeSymbol)}`} className="stock-chart-tab">AI 추천</Link>
            <Link to="/ai-invest" className="stock-chart-tab">AI 투자</Link>
          </div>

          <div className="toss-stock-action-grid">
            <Link to={`/ai-recommend?symbol=${encodeURIComponent(activeSymbol)}`} className="toss-action-card">
              <span>AI Recommend</span>
              <strong>{activePreset.label} 전략 보기</strong>
            </Link>
            <Link to="/kr-stocks" className="toss-action-card">
              <span>KR Stock</span>
              <strong>국내 주식 보기</strong>
            </Link>
          </div>

          <div className="stock-tv-head">
            <div>
              <p className="section-label">Real-Time Chart</p>
              <h1>{activePreset.label}</h1>
              <p className="muted stock-tv-copy">{activeSymbol}</p>
            </div>
            <div className="stock-live-mini-pill">{activePreset.summary}</div>
          </div>

          <StockMarketWarning message="KRX (Korea) is not supported in this TradingView widget. The chart may fail to load or show incomplete data." />

          <TradingViewAdvancedChart symbol={activeSymbol} allowSymbolChange theme={theme} />
        </article>

        <aside className="detail-stack">
          <section className="panel toss-side-panel">
            <div className="panel-head compact">
              <div>
                <p className="section-label">Quick Route</p>
                <h2>빠른 이동</h2>
              </div>
            </div>
            <div className="toss-route-list">
              <Link to={`/ai-recommend?symbol=${encodeURIComponent(activeSymbol)}`} className="toss-route-item">
                <strong>AI 추천</strong>
                <span>{activePreset.label}</span>
              </Link>
              <Link to="/ai-invest" className="toss-route-item">
                <strong>AI 투자 분석</strong>
                <span>시장 해석</span>
              </Link>
              <Link to="/words" className="toss-route-item">
                <strong>용어 검색</strong>
                <span>경제 사전</span>
              </Link>
            </div>
          </section>

          <section className="panel toss-side-panel">
            <div className="panel-head compact">
              <div>
                <p className="section-label">Market Note</p>
                <h2>현재 선택 종목</h2>
              </div>
            </div>
            <div className="toss-note-card">
              <strong>{activePreset.label}</strong>
              <p>{activePreset.summary}</p>
              <span>{activeSymbol}</span>
            </div>
          </section>
        </aside>
      </section>

      <StockAdvisorWidget symbol={activeSymbol} />
    </div>
  );
}
