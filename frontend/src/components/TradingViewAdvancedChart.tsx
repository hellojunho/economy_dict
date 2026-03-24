import { useEffect, useMemo, useRef } from 'react';

type TradingViewAdvancedChartProps = {
  symbol: string;
  allowSymbolChange?: boolean;
};

declare global {
  interface Window {
    TradingView?: unknown;
  }
}

export default function TradingViewAdvancedChart({
  symbol,
  allowSymbolChange = true
}: TradingViewAdvancedChartProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const tradingViewSymbol = useMemo(() => symbol.trim().toUpperCase(), [symbol]);
  const widgetId = useMemo(
    () => `tradingview_${symbol.replace(/[^0-9A-Za-z]/g, '').toLowerCase() || 'krx'}`,
    [symbol]
  );

  useEffect(() => {
    const container = containerRef.current;
    if (!container) {
      return undefined;
    }

    container.innerHTML = '';

    const widgetHost = document.createElement('div');
    widgetHost.className = 'tradingview-widget-container__widget';
    widgetHost.id = widgetId;

    const copyright = document.createElement('div');
    copyright.className = 'tradingview-widget-copyright';

    const link = document.createElement('a');
    link.href = `https://www.tradingview.com/symbols/${tradingViewSymbol.replace(':', '-')}/`;
    link.rel = 'noopener noreferrer';
    link.target = '_blank';
    link.textContent = `${tradingViewSymbol} on TradingView`;

    const span = document.createElement('span');
    span.className = 'blue-text';
    span.appendChild(link);
    copyright.appendChild(span);

    const script = document.createElement('script');
    script.src = 'https://s3.tradingview.com/external-embedding/embed-widget-advanced-chart.js';
    script.async = true;
    script.type = 'text/javascript';
    script.text = JSON.stringify({
      autosize: true,
      symbol: tradingViewSymbol,
      interval: 'D',
      timezone: 'Asia/Seoul',
      theme: 'light',
      style: '1',
      locale: 'kr',
      allow_symbol_change: allowSymbolChange,
      hide_top_toolbar: false,
      hide_legend: false,
      withdateranges: true,
      save_image: false,
      support_host: 'https://www.tradingview.com'
    });

    container.appendChild(widgetHost);
    container.appendChild(copyright);
    container.appendChild(script);

    return () => {
      container.innerHTML = '';
    };
  }, [allowSymbolChange, tradingViewSymbol, widgetId]);

  return <div ref={containerRef} className="tradingview-widget-container stock-tv-widget" />;
}
