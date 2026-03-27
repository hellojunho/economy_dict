import { useEffect, useRef } from 'react';
import {
  CandlestickSeries,
  ColorType,
  CrosshairMode,
  LineStyle,
  createChart,
  type IChartApi,
  type ISeriesApi,
  type UTCTimestamp
} from 'lightweight-charts';

type KrStockCandleChartProps = {
  candles: Array<{
    time: number;
    open: number;
    high: number;
    low: number;
    close: number;
  }>;
  showIntradayTime: boolean;
  theme?: 'dark' | 'light';
};

export default function KrStockCandleChart({ candles, showIntradayTime, theme = 'dark' }: KrStockCandleChartProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const seriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null);

  useEffect(() => {
    if (!containerRef.current) {
      return undefined;
    }

    const isDark = theme === 'dark';
    const bg = isDark ? '#0d1424' : '#ffffff';
    const textColor = isDark ? 'rgba(255,255,255,0.5)' : '#6b7280';
    const gridColor = isDark ? 'rgba(255,255,255,0.06)' : '#f3f4f6';
    const borderColor = isDark ? 'rgba(255,255,255,0.08)' : '#e5e7eb';
    const upColor = isDark ? '#22c55e' : '#111827';
    const downColor = isDark ? '#ef4444' : '#b91c1c';

    const chart = createChart(containerRef.current, {
      width: containerRef.current.clientWidth,
      height: 440,
      layout: {
        background: { type: ColorType.Solid, color: bg },
        textColor,
        fontFamily: '"Inter", "Pretendard", sans-serif'
      },
      grid: {
        vertLines: { color: gridColor, style: LineStyle.Solid },
        horzLines: { color: gridColor, style: LineStyle.Solid }
      },
      crosshair: {
        mode: CrosshairMode.Normal
      },
      rightPriceScale: {
        borderColor
      },
      timeScale: {
        borderColor,
        timeVisible: showIntradayTime,
        secondsVisible: false
      }
    });

    const series = chart.addSeries(CandlestickSeries, {
      upColor,
      downColor,
      borderVisible: false,
      wickUpColor: upColor,
      wickDownColor: downColor
    });

    chartRef.current = chart;
    seriesRef.current = series;

    const handleResize = () => {
      if (!containerRef.current || !chartRef.current) {
        return;
      }
      chartRef.current.applyOptions({
        width: containerRef.current.clientWidth
      });
    };

    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      chart.remove();
      chartRef.current = null;
      seriesRef.current = null;
    };
  }, [showIntradayTime, theme]);

  useEffect(() => {
    if (!seriesRef.current || !chartRef.current) {
      return;
    }

    seriesRef.current.setData(
      candles.map((item) => ({
        time: item.time as UTCTimestamp,
        open: item.open,
        high: item.high,
        low: item.low,
        close: item.close
      }))
    );
    chartRef.current.timeScale().fitContent();
  }, [candles]);

  return <div ref={containerRef} className="stock-chart-surface" />;
}
