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

type CandlePoint = {
  time: number;
  open: number;
  high: number;
  low: number;
  close: number;
};

type StockCandleChartProps = {
  candles: CandlePoint[];
  mode: 'intraday' | 'daily';
};

export default function StockCandleChart({ candles, mode }: StockCandleChartProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const seriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null);

  useEffect(() => {
    if (!containerRef.current) {
      return undefined;
    }

    const chart = createChart(containerRef.current, {
      width: containerRef.current.clientWidth,
      height: 420,
      layout: {
        background: { type: ColorType.Solid, color: '#ffffff' },
        textColor: '#6b7280',
        fontFamily: '"Inter", "Pretendard", sans-serif'
      },
      grid: {
        vertLines: { color: '#f3f4f6', style: LineStyle.Solid },
        horzLines: { color: '#f3f4f6', style: LineStyle.Solid }
      },
      crosshair: {
        mode: CrosshairMode.Normal
      },
      rightPriceScale: {
        borderColor: '#e5e7eb'
      },
      timeScale: {
        borderColor: '#e5e7eb',
        timeVisible: mode === 'intraday',
        secondsVisible: false
      }
    });

    const series = chart.addSeries(CandlestickSeries, {
      upColor: '#111827',
      downColor: '#b91c1c',
      borderVisible: false,
      wickUpColor: '#111827',
      wickDownColor: '#b91c1c',
      priceLineVisible: true,
      lastValueVisible: true
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
  }, [mode]);

  useEffect(() => {
    if (!seriesRef.current || !chartRef.current) {
      return;
    }

    seriesRef.current.setData(
      candles.map((candle) => ({
        time: candle.time as UTCTimestamp,
        open: candle.open,
        high: candle.high,
        low: candle.low,
        close: candle.close
      }))
    );
    chartRef.current.timeScale().fitContent();
  }, [candles]);

  return <div ref={containerRef} className="stock-chart-surface" />;
}
