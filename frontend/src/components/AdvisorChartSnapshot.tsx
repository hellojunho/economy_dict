import { useEffect, useMemo, useRef, useState } from 'react';
import {
  CandlestickSeries,
  ColorType,
  CrosshairMode,
  LineSeries,
  LineStyle,
  createChart,
  type IChartApi,
  type ISeriesApi,
  type UTCTimestamp
} from 'lightweight-charts';
import client from '../api/client';

type KrStockCandle = {
  time: number;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
};

type KrStockQuote = {
  lastPrice: number | null;
};

type KrStockSnapshot = {
  fetchedAt: string;
  symbol: string;
  name: string;
  quote: KrStockQuote;
  intradayCandles: KrStockCandle[];
  dailyCandles: KrStockCandle[];
};

type ChartAnalysis = {
  symbol: string;
  name: string;
  fetchedAt: string;
  timeframeLabel: string;
  windowLabel: string;
  candles: KrStockCandle[];
  currentPrice: number;
  oneYearLow: number;
  oneYearHigh: number;
  windowLow: number;
  windowHigh: number;
  midpoint: number;
  support: number;
  resistance: number;
  boxLow: number;
  boxHigh: number;
  trendStart: number;
  trendEnd: number;
  trendDirection: 'up' | 'down' | 'flat';
};

type AdvisorChartSnapshotProps = {
  symbol: string;
  tradeStyle: string;
};

function extractKrCode(symbol: string) {
  const normalized = symbol.trim().toUpperCase();
  if (/^\d{6}$/.test(normalized)) {
    return normalized;
  }

  const matched = normalized.match(/^(?:KRX|KOSPI|KOSDAQ):(\d{6})$/);
  return matched?.[1] ?? null;
}

function average(values: number[]) {
  if (values.length === 0) {
    return 0;
  }
  return values.reduce((sum, value) => sum + value, 0) / values.length;
}

function lastItems<T>(items: T[], count: number) {
  if (items.length <= count) {
    return items;
  }
  return items.slice(items.length - count);
}

function aggregateIntraday(candles: KrStockCandle[], minuteBucket: number) {
  const grouped = new Map<number, KrStockCandle[]>();

  candles.forEach((candle) => {
    const bucket = Math.floor(candle.time / (minuteBucket * 60)) * minuteBucket * 60;
    const current = grouped.get(bucket) ?? [];
    current.push(candle);
    grouped.set(bucket, current);
  });

  return [...grouped.entries()]
    .sort((left, right) => left[0] - right[0])
    .map(([bucket, bucketCandles]) => {
      const ordered = bucketCandles.sort((left, right) => left.time - right.time);
      return {
        time: bucket,
        open: ordered[0].open,
        high: Math.max(...ordered.map((item) => item.high)),
        low: Math.min(...ordered.map((item) => item.low)),
        close: ordered[ordered.length - 1].close,
        volume: ordered.reduce((sum, item) => sum + item.volume, 0)
      };
    });
}

function aggregateDaily(candles: KrStockCandle[], mode: 'weekly' | 'monthly') {
  const grouped = new Map<string, KrStockCandle[]>();

  candles.forEach((candle) => {
    const date = new Date(candle.time * 1000);
    const year = date.getUTCFullYear();
    const month = date.getUTCMonth() + 1;
    const key =
      mode === 'monthly'
        ? `${year}-${String(month).padStart(2, '0')}`
        : `${year}-W${getWeekBucket(date)}`;
    const current = grouped.get(key) ?? [];
    current.push(candle);
    grouped.set(key, current);
  });

  return [...grouped.values()].map((group) => {
    const ordered = group.sort((left, right) => left.time - right.time);
    return {
      time: ordered[0].time,
      open: ordered[0].open,
      high: Math.max(...ordered.map((item) => item.high)),
      low: Math.min(...ordered.map((item) => item.low)),
      close: ordered[ordered.length - 1].close,
      volume: ordered.reduce((sum, item) => sum + item.volume, 0)
    };
  });
}

function getWeekBucket(date: Date) {
  const target = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
  const dayNumber = (target.getUTCDay() + 6) % 7;
  target.setUTCDate(target.getUTCDate() - dayNumber + 3);
  const firstThursday = new Date(Date.UTC(target.getUTCFullYear(), 0, 4));
  const diff = target.getTime() - firstThursday.getTime();
  return String(1 + Math.round(diff / 604800000)).padStart(2, '0');
}

function buildAnalysis(snapshot: KrStockSnapshot, tradeStyle: string): ChartAnalysis | null {
  const dailyCandles = snapshot.dailyCandles ?? [];
  const intradayCandles = snapshot.intradayCandles ?? [];
  if (dailyCandles.length === 0 && intradayCandles.length === 0) {
    return null;
  }

  const style = tradeStyle.trim();
  let timeframeLabel = '일봉';
  let windowLabel = '최근 1개월';
  let selectedCandles = lastItems(dailyCandles, 30);

  if (style === '초단타') {
    timeframeLabel = '1분봉';
    windowLabel = '최근 90분';
    selectedCandles = lastItems(intradayCandles, 90);
  } else if (style === '단타') {
    timeframeLabel = '3분봉';
    windowLabel = '최근 4시간';
    selectedCandles = lastItems(aggregateIntraday(intradayCandles, 3), 80);
  } else if (style === '장투' || style === '장기') {
    timeframeLabel = '주봉';
    windowLabel = '최근 1년';
    selectedCandles = lastItems(aggregateDaily(dailyCandles, 'weekly'), 52);
  }

  if (selectedCandles.length === 0) {
    return null;
  }

  const oneYearCandles = lastItems(dailyCandles, 252);
  const oneYearLow = Math.min(...oneYearCandles.map((item) => item.low));
  const oneYearHigh = Math.max(...oneYearCandles.map((item) => item.high));
  const windowLow = Math.min(...selectedCandles.map((item) => item.low));
  const windowHigh = Math.max(...selectedCandles.map((item) => item.high));
  const midpoint = (windowLow + windowHigh) / 2;
  const recentCandles = lastItems(selectedCandles, Math.min(selectedCandles.length, style === '스윙' ? 12 : 18));
  const support = average([...recentCandles].map((item) => item.low).sort((left, right) => left - right).slice(0, 3));
  const resistance = average([...recentCandles].map((item) => item.high).sort((left, right) => right - left).slice(0, 3));
  const range = Math.max(windowHigh - windowLow, 0.0001);
  const boxLow = midpoint - range * 0.18;
  const boxHigh = midpoint + range * 0.18;

  const closes = selectedCandles.map((item) => item.close);
  const meanX = (closes.length - 1) / 2;
  const meanY = average(closes);
  const numerator = closes.reduce((sum, close, index) => sum + (index - meanX) * (close - meanY), 0);
  const denominator = closes.reduce((sum, _close, index) => sum + (index - meanX) ** 2, 0) || 1;
  const slope = numerator / denominator;
  const intercept = meanY - slope * meanX;
  const trendStart = intercept;
  const trendEnd = intercept + slope * (closes.length - 1);
  const trendDirection = Math.abs(slope) < range * 0.001 ? 'flat' : slope > 0 ? 'up' : 'down';

  return {
    symbol: snapshot.symbol,
    name: snapshot.name,
    fetchedAt: snapshot.fetchedAt,
    timeframeLabel,
    windowLabel,
    candles: selectedCandles,
    currentPrice: snapshot.quote.lastPrice ?? selectedCandles[selectedCandles.length - 1].close,
    oneYearLow,
    oneYearHigh,
    windowLow,
    windowHigh,
    midpoint,
    support,
    resistance,
    boxLow,
    boxHigh,
    trendStart,
    trendEnd,
    trendDirection
  };
}

function formatPrice(value: number) {
  return `₩${new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 0 }).format(value)}`;
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(value));
}

export default function AdvisorChartSnapshot({ symbol, tradeStyle }: AdvisorChartSnapshotProps) {
  const krCode = useMemo(() => extractKrCode(symbol), [symbol]);
  const [snapshot, setSnapshot] = useState<KrStockSnapshot | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [snapshotUrl, setSnapshotUrl] = useState('');
  const chartRef = useRef<IChartApi | null>(null);
  const candleSeriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null);
  const trendSeriesRef = useRef<ISeriesApi<'Line'> | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!krCode) {
      setSnapshot(null);
      setSnapshotUrl('');
      setError('');
      return;
    }

    let active = true;
    setLoading(true);
    setError('');

    client.get<KrStockSnapshot>(`/kr-stocks/${krCode}`)
      .then((response) => {
        if (active) {
          setSnapshot(response.data);
        }
      })
      .catch(() => {
        if (active) {
          setSnapshot(null);
          setSnapshotUrl('');
          setError('현재 자동 차트 스냅샷은 연결된 KRX 데이터가 있을 때만 생성됩니다.');
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [krCode]);

  const analysis = useMemo(() => {
    if (!snapshot) {
      return null;
    }
    return buildAnalysis(snapshot, tradeStyle);
  }, [snapshot, tradeStyle]);

  useEffect(() => {
    if (!containerRef.current || !analysis) {
      return undefined;
    }

    const chart = createChart(containerRef.current, {
      width: containerRef.current.clientWidth,
      height: 360,
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
        timeVisible: analysis.timeframeLabel !== '주봉' && analysis.timeframeLabel !== '월봉',
        secondsVisible: false
      }
    });

    const candleSeries = chart.addSeries(CandlestickSeries, {
      upColor: '#111827',
      downColor: '#b91c1c',
      borderVisible: false,
      wickUpColor: '#111827',
      wickDownColor: '#b91c1c'
    });
    const trendSeries = chart.addSeries(LineSeries, {
      color: analysis.trendDirection === 'up' ? '#2563eb' : analysis.trendDirection === 'down' ? '#dc2626' : '#6b7280',
      lineWidth: 2,
      crosshairMarkerVisible: false,
      lastValueVisible: false,
      priceLineVisible: false
    });

    chartRef.current = chart;
    candleSeriesRef.current = candleSeries;
    trendSeriesRef.current = trendSeries;

    candleSeries.setData(
      analysis.candles.map((item) => ({
        time: item.time as UTCTimestamp,
        open: item.open,
        high: item.high,
        low: item.low,
        close: item.close
      }))
    );

    trendSeries.setData([
      { time: analysis.candles[0].time as UTCTimestamp, value: analysis.trendStart },
      { time: analysis.candles[analysis.candles.length - 1].time as UTCTimestamp, value: analysis.trendEnd }
    ]);

    const lines = [
      { price: analysis.currentPrice, color: '#111827', title: '현재가', style: LineStyle.Solid },
      { price: analysis.support, color: '#16a34a', title: '지지', style: LineStyle.Dashed },
      { price: analysis.resistance, color: '#dc2626', title: '저항', style: LineStyle.Dashed },
      { price: analysis.boxLow, color: '#6366f1', title: '박스 하단', style: LineStyle.Dotted },
      { price: analysis.boxHigh, color: '#6366f1', title: '박스 상단', style: LineStyle.Dotted },
      { price: analysis.oneYearLow, color: '#0f766e', title: '1Y 저가', style: LineStyle.SparseDotted },
      { price: analysis.oneYearHigh, color: '#92400e', title: '1Y 고가', style: LineStyle.SparseDotted }
    ];

    lines.forEach((item) => {
      candleSeries.createPriceLine({
        price: item.price,
        color: item.color,
        lineWidth: 1,
        lineStyle: item.style,
        axisLabelVisible: true,
        title: item.title
      });
    });

    chart.timeScale().fitContent();

    const syncSnapshot = () => {
      window.requestAnimationFrame(() => {
        setSnapshotUrl(chart.takeScreenshot(true, false).toDataURL('image/png'));
      });
    };

    syncSnapshot();

    const handleResize = () => {
      if (!containerRef.current) {
        return;
      }
      chart.applyOptions({ width: containerRef.current.clientWidth });
      chart.timeScale().fitContent();
      syncSnapshot();
    };

    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      chart.remove();
      chartRef.current = null;
      candleSeriesRef.current = null;
      trendSeriesRef.current = null;
    };
  }, [analysis]);

  if (!krCode) {
    return (
      <section className="advisor-chart-panel panel">
        <div className="advisor-chart-panel-head">
          <div>
            <p className="section-label">AI Chart Snapshot</p>
            <h3>자동 스냅샷 준비 중</h3>
          </div>
        </div>
        <p className="panel-copy">
          현재 자동 차트 스냅샷과 오버레이는 키움 데이터가 연결된 KRX 종목에서만 생성됩니다. 글로벌 종목은 캔들 데이터 소스가 아직 연결되어 있지 않아 텍스트 분석만 우선 제공합니다.
        </p>
      </section>
    );
  }

  return (
    <section className="advisor-chart-panel panel">
      <div className="advisor-chart-panel-head">
        <div>
          <p className="section-label">AI Chart Snapshot</p>
          <h3>{analysis ? `${analysis.name} ${analysis.timeframeLabel}` : `${krCode} 차트 준비 중`}</h3>
        </div>
        {analysis && snapshotUrl && (
          <a
            className="button button-secondary"
            href={snapshotUrl}
            download={`${analysis.symbol}-${analysis.timeframeLabel}-snapshot.png`}
          >
            Snapshot PNG
          </a>
        )}
      </div>

      {loading && <p className="muted">차트 스냅샷을 준비하는 중입니다.</p>}
      {!loading && error && <p className="form-message error-text">{error}</p>}

      {analysis && (
        <>
          <div className="advisor-chart-summary">
            <div>
              <span>기준봉</span>
              <strong>{analysis.timeframeLabel}</strong>
            </div>
            <div>
              <span>현재가</span>
              <strong>{formatPrice(analysis.currentPrice)}</strong>
            </div>
            <div>
              <span>1년 범위</span>
              <strong>{formatPrice(analysis.oneYearLow)} ~ {formatPrice(analysis.oneYearHigh)}</strong>
            </div>
            <div>
              <span>{analysis.windowLabel}</span>
              <strong>{formatPrice(analysis.windowLow)} ~ {formatPrice(analysis.windowHigh)}</strong>
            </div>
          </div>

          <div ref={containerRef} className="advisor-chart-surface" />

          <div className="advisor-chart-legend">
            <span>지지 {formatPrice(analysis.support)}</span>
            <span>저항 {formatPrice(analysis.resistance)}</span>
            <span>중간값 {formatPrice(analysis.midpoint)}</span>
            <span>박스권 {formatPrice(analysis.boxLow)} ~ {formatPrice(analysis.boxHigh)}</span>
            <span>추세선 {analysis.trendDirection === 'up' ? '상승' : analysis.trendDirection === 'down' ? '하락' : '횡보'}</span>
            <span>기준 시각 {formatDateTime(analysis.fetchedAt)}</span>
          </div>

          {snapshotUrl && (
            <div className="advisor-chart-image-wrap">
              <img
                src={snapshotUrl}
                alt={`${analysis.symbol} ${analysis.timeframeLabel} AI snapshot`}
                className="advisor-chart-image"
              />
            </div>
          )}
        </>
      )}
    </section>
  );
}
