import StockMarketWarning from '../components/StockMarketWarning';
import TradingViewAdvancedChart from '../components/TradingViewAdvancedChart';

const DEFAULT_SYMBOL = 'NASDAQ:TSLA';

export default function Stock() {
  return (
    <div className="site-frame page-stack">
      <section className="panel stock-tv-panel">
        <StockMarketWarning message="KRX (Korea) is not supported in this TradingView widget. The chart may fail to load or show incomplete data." />

        <TradingViewAdvancedChart symbol={DEFAULT_SYMBOL} />
      </section>
    </div>
  );
}
