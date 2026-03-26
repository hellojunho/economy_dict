import { useState } from 'react';
import StockAdvisorWidget from '../components/StockAdvisorWidget';
import StockMarketWarning from '../components/StockMarketWarning';
import TradingViewAdvancedChart from '../components/TradingViewAdvancedChart';
import { useThemeStore } from '../stores/themeStore';

const DEFAULT_SYMBOL = 'NASDAQ:TSLA';

export default function Stock() {
  const [activeSymbol, setActiveSymbol] = useState(DEFAULT_SYMBOL);
  const theme = useThemeStore((state) => state.theme);

  return (
    <div className="site-frame page-stack stock-page">
      <section className="panel stock-tv-panel">
        <div className="stock-tv-head">
          <div>
            <p className="section-label">Global Stock</p>
            <h1>{activeSymbol}</h1>
          </div>
          <p className="muted stock-tv-copy">
            우측 하단 전략 상담 버튼을 누르면 AI Recommend 페이지로 이동해 현재 종목 기준 투자 전략 대화를 이어갈 수 있습니다.
          </p>
        </div>

        <StockMarketWarning message="KRX (Korea) is not supported in this TradingView widget. The chart may fail to load or show incomplete data." />

        <TradingViewAdvancedChart symbol={activeSymbol} allowSymbolChange={false} theme={theme} />
      </section>

      <StockAdvisorWidget symbol={activeSymbol} />
    </div>
  );
}
