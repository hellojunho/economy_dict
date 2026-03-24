import { useState } from 'react';
import StockAdvisorWidget from '../components/StockAdvisorWidget';
import StockMarketWarning from '../components/StockMarketWarning';
import TradingViewAdvancedChart from '../components/TradingViewAdvancedChart';

const DEFAULT_SYMBOL = 'NASDAQ:TSLA';

export default function Stock() {
  const [activeSymbol, setActiveSymbol] = useState(DEFAULT_SYMBOL);

  return (
    <div className="site-frame page-stack stock-page">
      <section className="panel stock-tv-panel">
        <div className="stock-tv-head">
          <div>
            <p className="section-label">Global Stock</p>
            <h1>{activeSymbol}</h1>
          </div>
          <p className="muted stock-tv-copy">
            우측 하단 전략 상담 탭에서 TradingView 심볼과 투자 성향을 입력하면 차트와 AI 분석이 함께 갱신됩니다.
          </p>
        </div>

        <StockMarketWarning message="KRX (Korea) is not supported in this TradingView widget. The chart may fail to load or show incomplete data." />

        <TradingViewAdvancedChart symbol={activeSymbol} allowSymbolChange={false} />
      </section>

      <StockAdvisorWidget symbol={activeSymbol} onSymbolChange={setActiveSymbol} />
    </div>
  );
}
