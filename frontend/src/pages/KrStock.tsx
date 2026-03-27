import { useEffect, useRef, useState } from 'react';
import client from '../api/client';
import InvestFeatureTabs from '../components/InvestFeatureTabs';
import KrStockCandleChart from '../components/KrStockCandleChart';
import StockMarketWarning from '../components/StockMarketWarning';
import { useThemeStore } from '../stores/themeStore';

type KrStockMetric = {
  label: string;
  value: string;
};

type KrStockSection = {
  title: string;
  rows: KrStockMetric[];
};

type KrStockCandle = {
  time: number;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
};

type KrStockQuote = {
  symbol: string;
  name: string;
  lastPrice: number | null;
  change: number | null;
  changeRate: number | null;
  open: number | null;
  high: number | null;
  low: number | null;
  previousClose: number | null;
  volume: number | null;
  tradeValue: number | null;
  tradeTimeLabel: string | null;
};

type KrStockOrderBookLevel = {
  price: number;
  quantity: number;
};

type KrStockOrderBook = {
  asks: KrStockOrderBookLevel[];
  bids: KrStockOrderBookLevel[];
  totalAskQuantity: number | null;
  totalBidQuantity: number | null;
  baseTime: string | null;
};

type KrStockSnapshot = {
  provider: string;
  market: string;
  symbol: string;
  name: string;
  fetchedAt: string;
  liveRefreshIntervalSeconds: number;
  quote: KrStockQuote;
  orderBook: KrStockOrderBook;
  intradayCandles: KrStockCandle[];
  dailyCandles: KrStockCandle[];
  sections: KrStockSection[];
};

type KrStockRealtime = {
  symbol: string;
  fetchedAt: string;
  quote: KrStockQuote;
  orderBook: KrStockOrderBook;
};

type KrStockApiError = {
  summary: string;
  details: string[];
};

type StockSymbolOption = {
  symbol: string;
  description: string;
  exchange: string;
  type: string;
  country: string;
  directViewOnly: boolean;
  directViewOnlyMessage: string;
};

type CandleInterval =
  | '1m'
  | '3m'
  | '5m'
  | '10m'
  | '15m'
  | '30m'
  | '1h'
  | '4h'
  | '6h'
  | '12h'
  | '1d'
  | '7d'
  | '1mo'
  | '1y';

type CandleIntervalOption = {
  value: CandleInterval;
  label: string;
  source: 'intraday' | 'daily';
  bucketSeconds?: number;
  calendarBucket?: '7d' | '1mo' | '1y';
};

const intervalOptions: CandleIntervalOption[] = [
  { value: '1m', label: '1분', source: 'intraday', bucketSeconds: 60 },
  { value: '3m', label: '3분', source: 'intraday', bucketSeconds: 60 * 3 },
  { value: '5m', label: '5분', source: 'intraday', bucketSeconds: 60 * 5 },
  { value: '10m', label: '10분', source: 'intraday', bucketSeconds: 60 * 10 },
  { value: '15m', label: '15분', source: 'intraday', bucketSeconds: 60 * 15 },
  { value: '30m', label: '30분', source: 'intraday', bucketSeconds: 60 * 30 },
  { value: '1h', label: '1시간', source: 'intraday', bucketSeconds: 60 * 60 },
  { value: '4h', label: '4시간', source: 'intraday', bucketSeconds: 60 * 60 * 4 },
  { value: '6h', label: '6시간', source: 'intraday', bucketSeconds: 60 * 60 * 6 },
  { value: '12h', label: '12시간', source: 'intraday', bucketSeconds: 60 * 60 * 12 },
  { value: '1d', label: '1일', source: 'daily' },
  { value: '7d', label: '7일', source: 'daily', calendarBucket: '7d' },
  { value: '1mo', label: '1달', source: 'daily', calendarBucket: '1mo' },
  { value: '1y', label: '1년', source: 'daily', calendarBucket: '1y' }
];

function formatPrice(value: number | null) {
  if (value === null || Number.isNaN(value)) {
    return '-';
  }
  return `₩${new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 2 }).format(value)}`;
}

function formatWhole(value: number | null) {
  if (value === null || Number.isNaN(value)) {
    return '-';
  }
  return new Intl.NumberFormat('ko-KR').format(value);
}

function formatPercent(value: number | null) {
  if (value === null || Number.isNaN(value)) {
    return '-';
  }
  return `${value > 0 ? '+' : ''}${value.toFixed(2)}%`;
}

function formatSigned(value: number | null) {
  if (value === null || Number.isNaN(value)) {
    return '-';
  }
  return `${value > 0 ? '+' : ''}${new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 2 }).format(value)}`;
}

function formatDateTime(value: string | null) {
  if (!value) {
    return '-';
  }
  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeStyle: 'medium'
  }).format(new Date(value));
}

function buildError(summary: string, details: string[] = []): KrStockApiError {
  return { summary, details };
}

function getErrorInfo(error: unknown): KrStockApiError {
  if (typeof error === 'object' && error !== null) {
    const response = error as {
      message?: string;
      response?: {
        status?: number;
        data?: {
          code?: string;
          message?: string;
          details?: string[];
          traceId?: string;
        };
      };
    };
    const payload = response.response?.data;
    if (payload?.message) {
      const prefix = [`HTTP ${response.response?.status ?? '-'}`, payload.code].filter(Boolean).join(' ');
      const details = [...(payload.details ?? [])];
      if (payload.traceId) {
        details.push(`traceId=${payload.traceId}`);
      }
      return buildError(prefix ? `${prefix} - ${payload.message}` : payload.message, details);
    }
    if (response.message) {
      return buildError(response.message);
    }
  }
  return buildError('국내주식 데이터를 불러오지 못했습니다.', [
    '키움 API 키, 허용 IP, 종목코드, 백엔드 로그를 확인하세요.'
  ]);
}

function buildKrStockWebSocketUrl(symbol: string) {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  return `${protocol}://${window.location.host}/api/ws/kr-stocks/${encodeURIComponent(symbol)}`;
}

function normalizeKrSymbol(symbol: string) {
  const raw = symbol.includes(':') ? symbol.substring(symbol.indexOf(':') + 1) : symbol;
  return raw.replace(/[^0-9]/g, '').padStart(6, '0').slice(-6);
}

function isKrStockOption(option: StockSymbolOption) {
  const symbol = option.symbol.toUpperCase();
  const exchange = option.exchange.toUpperCase();
  const country = option.country.toUpperCase();
  return symbol.startsWith('KRX:') || exchange === 'KRX' || country === 'KR' || option.directViewOnly;
}

function mergeRealtimeCandle(candles: KrStockCandle[], quote: KrStockQuote) {
  if (!quote.lastPrice || !quote.tradeTimeLabel) {
    return candles;
  }

  const parts = quote.tradeTimeLabel.split(':').map((item) => Number(item));
  if (parts.length < 2 || parts.some((item) => Number.isNaN(item))) {
    return candles;
  }

  const now = new Date();
  now.setHours(parts[0], parts[1], parts[2] ?? 0, 0);
  const bucket = Math.floor(now.getTime() / 60000) * 60;

  const next = [...candles];
  const last = next[next.length - 1];

  if (!last) {
    next.push({
      time: bucket,
      open: quote.lastPrice,
      high: quote.lastPrice,
      low: quote.lastPrice,
      close: quote.lastPrice,
      volume: 0
    });
    return next;
  }

  if (bucket < last.time) {
    return next;
  }

  if (bucket === last.time) {
    next[next.length - 1] = {
      ...last,
      high: Math.max(last.high, quote.lastPrice),
      low: Math.min(last.low, quote.lastPrice),
      close: quote.lastPrice
    };
    return next;
  }

  next.push({
    time: bucket,
    open: last.close,
    high: Math.max(last.close, quote.lastPrice),
    low: Math.min(last.close, quote.lastPrice),
    close: quote.lastPrice,
    volume: 0
  });

  return next.slice(-900);
}

function emptyOrderBook(): KrStockOrderBook {
  return {
    asks: [],
    bids: [],
    totalAskQuantity: null,
    totalBidQuantity: null,
    baseTime: null
  };
}

function createRealtimeSnapshot(symbol: string, payload: KrStockRealtime): KrStockSnapshot {
  return {
    provider: 'Kiwoom REST API',
    market: 'KRX',
    symbol,
    name: payload.quote.name || symbol,
    fetchedAt: payload.fetchedAt,
    liveRefreshIntervalSeconds: 2,
    quote: payload.quote,
    orderBook: payload.orderBook ?? emptyOrderBook(),
    intradayCandles: mergeRealtimeCandle([], payload.quote),
    dailyCandles: [],
    sections: []
  };
}

function renderErrorBlock(title: string, error: KrStockApiError | null) {
  if (!error) {
    return null;
  }
  return (
    <div className="form-message error-text">
      <strong>{title}:</strong> {error.summary}
      {error.details.map((detail) => (
        <div key={`${title}-${detail}`}>{detail}</div>
      ))}
    </div>
  );
}

function aggregateByFixedSeconds(candles: KrStockCandle[], bucketSeconds: number) {
  if (bucketSeconds <= 60) {
    return [...candles].sort((a, b) => a.time - b.time);
  }

  const sorted = [...candles].sort((a, b) => a.time - b.time);
  const aggregated: KrStockCandle[] = [];

  for (const candle of sorted) {
    const bucket = Math.floor(candle.time / bucketSeconds) * bucketSeconds;
    const last = aggregated[aggregated.length - 1];

    if (!last || last.time !== bucket) {
      aggregated.push({
        time: bucket,
        open: candle.open,
        high: candle.high,
        low: candle.low,
        close: candle.close,
        volume: candle.volume
      });
      continue;
    }

    last.high = Math.max(last.high, candle.high);
    last.low = Math.min(last.low, candle.low);
    last.close = candle.close;
    last.volume += candle.volume;
  }

  return aggregated;
}

function getKstBucketStart(time: number, bucket: '7d' | '1mo' | '1y') {
  const shifted = time + 9 * 60 * 60;

  if (bucket === '7d') {
    const span = 7 * 24 * 60 * 60;
    return Math.floor(shifted / span) * span - 9 * 60 * 60;
  }

  const shiftedDate = new Date(shifted * 1000);
  const year = shiftedDate.getUTCFullYear();

  if (bucket === '1mo') {
    return Date.UTC(year, shiftedDate.getUTCMonth(), 1) / 1000 - 9 * 60 * 60;
  }

  return Date.UTC(year, 0, 1) / 1000 - 9 * 60 * 60;
}

function aggregateByCalendarBucket(candles: KrStockCandle[], bucket: '7d' | '1mo' | '1y') {
  const sorted = [...candles].sort((a, b) => a.time - b.time);
  const aggregated: KrStockCandle[] = [];

  for (const candle of sorted) {
    const bucketStart = getKstBucketStart(candle.time, bucket);
    const last = aggregated[aggregated.length - 1];

    if (!last || last.time !== bucketStart) {
      aggregated.push({
        time: bucketStart,
        open: candle.open,
        high: candle.high,
        low: candle.low,
        close: candle.close,
        volume: candle.volume
      });
      continue;
    }

    last.high = Math.max(last.high, candle.high);
    last.low = Math.min(last.low, candle.low);
    last.close = candle.close;
    last.volume += candle.volume;
  }

  return aggregated;
}

function buildChartData(snapshot: KrStockSnapshot | null, interval: CandleInterval) {
  if (!snapshot) {
    return [];
  }

  const option = intervalOptions.find((item) => item.value === interval);
  if (!option) {
    return [];
  }

  if (option.source === 'intraday') {
    return aggregateByFixedSeconds(snapshot.intradayCandles ?? [], option.bucketSeconds ?? 60);
  }

  if (option.calendarBucket) {
    return aggregateByCalendarBucket(snapshot.dailyCandles ?? [], option.calendarBucket);
  }

  return [...(snapshot.dailyCandles ?? [])].sort((a, b) => a.time - b.time);
}

function getIntervalLabel(interval: CandleInterval) {
  return intervalOptions.find((item) => item.value === interval)?.label ?? interval;
}

function isIntradayInterval(interval: CandleInterval) {
  return intervalOptions.find((item) => item.value === interval)?.source === 'intraday';
}

export default function KrStock() {
  const theme = useThemeStore((state: { theme: 'dark' | 'light' }) => state.theme);
  const symbolPickerRef = useRef<HTMLDivElement | null>(null);
  const [activeSymbol, setActiveSymbol] = useState('005930');
  const [selectedSymbolLabel, setSelectedSymbolLabel] = useState('Samsung Electronics Co., Ltd.');
  const [selectedInterval, setSelectedInterval] = useState<CandleInterval>('1m');
  const [snapshot, setSnapshot] = useState<KrStockSnapshot | null>(null);
  const [requestError, setRequestError] = useState<KrStockApiError | null>(null);
  const [streamError, setStreamError] = useState<KrStockApiError | null>(null);
  const [symbolError, setSymbolError] = useState('');
  const [loading, setLoading] = useState(true);
  const [streamConnected, setStreamConnected] = useState(false);
  const [symbolMenuOpen, setSymbolMenuOpen] = useState(false);
  const [symbolQuery, setSymbolQuery] = useState('');
  const [symbolLoading, setSymbolLoading] = useState(false);
  const [symbolOptions, setSymbolOptions] = useState<StockSymbolOption[]>([]);

  useEffect(() => {
    let active = true;
    const timer = window.setTimeout(() => {
      setSymbolLoading(true);
      setSymbolError('');

      client.get<StockSymbolOption[]>('/stocks/symbols', {
        params: symbolQuery.trim() ? { query: symbolQuery.trim() } : {}
      })
        .then((response) => {
          if (!active) {
            return;
          }
          setSymbolOptions(response.data.filter(isKrStockOption).slice(0, 24));
        })
        .catch(() => {
          if (!active) {
            return;
          }
          setSymbolOptions([]);
          setSymbolError('종목 목록을 불러오지 못했습니다. 잠시 후 다시 시도하세요.');
        })
        .finally(() => {
          if (active) {
            setSymbolLoading(false);
          }
        });
    }, 180);

    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [symbolQuery]);

  useEffect(() => {
    const handlePointerDown = (event: MouseEvent) => {
      if (!symbolPickerRef.current) {
        return;
      }
      if (!symbolPickerRef.current.contains(event.target as Node)) {
        setSymbolMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handlePointerDown);
    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
    };
  }, []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setRequestError(null);
    setStreamError(null);

    client.get<KrStockSnapshot>(`/kr-stocks/${activeSymbol}`)
      .then((response) => {
        if (!active) {
          return;
        }
        setSnapshot(response.data);
        setSelectedSymbolLabel(response.data.name || selectedSymbolLabel);
      })
      .catch((error) => {
        if (!active) {
          return;
        }
        setSnapshot(null);
        setRequestError(getErrorInfo(error));
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [activeSymbol]);

  useEffect(() => {
    const socket = new WebSocket(buildKrStockWebSocketUrl(activeSymbol));

    socket.onopen = () => {
      setStreamConnected(true);
      setStreamError(null);
    };

    socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data) as KrStockRealtime;
        setSnapshot((current) => {
          if (!current) {
            return createRealtimeSnapshot(activeSymbol, payload);
          }
          return {
            ...current,
            fetchedAt: payload.fetchedAt,
            quote: payload.quote,
            orderBook: payload.orderBook,
            intradayCandles: mergeRealtimeCandle(current.intradayCandles, payload.quote)
          };
        });
      } catch {
        setStreamError(buildError('실시간 시세 메시지를 해석하지 못했습니다.'));
      }
    };

    socket.onerror = () => {
      setStreamConnected(false);
      setStreamError(buildError('실시간 WebSocket 연결에 실패했습니다.', [
        '브라우저 개발자도구의 Network > WS와 백엔드 로그를 함께 확인하세요.'
      ]));
    };

    socket.onclose = () => {
      setStreamConnected(false);
    };

    return () => {
      socket.close();
    };
  }, [activeSymbol]);

  useEffect(() => {
    const matched = symbolOptions.find((option) => normalizeKrSymbol(option.symbol) === activeSymbol);
    if (matched) {
      setSelectedSymbolLabel(matched.description);
    }
  }, [activeSymbol, symbolOptions]);

  const handleSelectSymbol = (option: StockSymbolOption) => {
    const nextSymbol = normalizeKrSymbol(option.symbol);
    setSelectedSymbolLabel(option.description);
    setActiveSymbol(nextSymbol);
    setSymbolMenuOpen(false);
    setSymbolQuery('');
  };

  const chartData = buildChartData(snapshot, selectedInterval);

  return (
    <div className="site-frame page-stack">
      <InvestFeatureTabs />

      <section className="panel stock-hero">
        <div className="stock-topbar">
          <div>
            <p className="section-label">KR Stock</p>
            <h1>키움 국내주식 차트</h1>
            <p className="panel-copy">
              TradingView 스타일의 기간 탭과 종목 드롭다운으로 차트를 바로 전환합니다.
            </p>
          </div>
          <div className="stock-live-pill">
            <strong>{snapshot?.provider ?? 'Kiwoom'}</strong>
            <span>{streamConnected ? 'WebSocket Live' : 'WebSocket Offline'}</span>
          </div>
        </div>

        <StockMarketWarning message="키움 API는 등록된 허용 IP에서만 요청됩니다. 이 서버 IP가 키움 콘솔에 등록되어 있어야 실제 데이터가 내려옵니다." />

        <div className="kr-stock-toolbar">
          <div ref={symbolPickerRef} className="kr-stock-symbol-picker">
            <button
              type="button"
              className="kr-stock-symbol-trigger"
              onClick={() => setSymbolMenuOpen((current) => !current)}
            >
              <div className="kr-stock-symbol-trigger-copy">
                <span>종목 선택</span>
                <strong>{snapshot?.name ?? selectedSymbolLabel}</strong>
                <small>{activeSymbol}</small>
              </div>
              <span className="kr-stock-symbol-trigger-caret">{symbolMenuOpen ? '▴' : '▾'}</span>
            </button>

            {symbolMenuOpen && (
              <div className="kr-stock-symbol-menu">
                <input
                  className="kr-stock-symbol-search"
                  value={symbolQuery}
                  onChange={(event) => setSymbolQuery(event.target.value)}
                  placeholder="종목명 또는 코드 검색"
                />

                <div className="kr-stock-symbol-list">
                  {symbolLoading && <p className="muted">종목 목록을 불러오는 중입니다.</p>}
                  {!symbolLoading && symbolError && <p className="error-text">{symbolError}</p>}
                  {!symbolLoading && !symbolError && symbolOptions.length === 0 && (
                    <p className="muted">표시할 종목이 없습니다.</p>
                  )}

                  {symbolOptions.map((option) => {
                    const optionSymbol = normalizeKrSymbol(option.symbol);
                    const active = optionSymbol === activeSymbol;
                    return (
                      <button
                        key={option.symbol}
                        type="button"
                        className={`kr-stock-symbol-option${active ? ' active' : ''}`}
                        onClick={() => handleSelectSymbol(option)}
                      >
                        <div className="kr-stock-symbol-option-copy">
                          <strong>{option.description}</strong>
                          <span>{option.exchange} · {option.type || 'stock'}</span>
                        </div>
                        <span className="kr-stock-symbol-option-code">{optionSymbol}</span>
                      </button>
                    );
                  })}
                </div>
              </div>
            )}
          </div>

          <div className="stock-chart-tabs">
            {intervalOptions.map((option) => (
              <button
                key={option.value}
                type="button"
                className={`stock-chart-tab${selectedInterval === option.value ? ' active' : ''}`}
                onClick={() => setSelectedInterval(option.value)}
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>

        {renderErrorBlock('Snapshot Error', requestError)}
        {renderErrorBlock('WebSocket Error', streamError)}
      </section>

      <section className="content-grid columns-2-1 stock-summary-layout">
        <article className="panel stock-chart-panel">
          <div className="panel-head stock-chart-head">
            <div>
              <p className="section-label">Candles</p>
              <h2>{snapshot?.name ?? selectedSymbolLabel}</h2>
              <p className="panel-copy">
                {snapshot ? `${snapshot.market} · ${snapshot.symbol} · ${getIntervalLabel(selectedInterval)} 봉` : '차트 데이터를 불러오는 중입니다.'}
              </p>
            </div>
          </div>

          {chartData.length > 0 ? (
            <KrStockCandleChart
              candles={chartData}
              showIntradayTime={isIntradayInterval(selectedInterval)}
              theme={theme}
            />
          ) : (
            <div className="stock-chart-empty">
              <p className="muted">표시할 캔들 데이터가 없습니다.</p>
              {requestError && <p className="error-text">{requestError.summary}</p>}
            </div>
          )}
        </article>

        <aside className="detail-stack">
          <section className="panel">
            <div className="panel-head compact">
              <div>
                <p className="section-label">Snapshot</p>
                <h2>현재 시세</h2>
              </div>
            </div>

            <div className="stock-quote-primary">
              <strong>{formatPrice(snapshot?.quote.lastPrice ?? null)}</strong>
              <span className={(snapshot?.quote.change ?? 0) >= 0 ? 'success-text' : 'danger-text'}>
                {formatSigned(snapshot?.quote.change ?? null)} · {formatPercent(snapshot?.quote.changeRate ?? null)}
              </span>
            </div>

            <div className="compact-grid stock-mini-grid">
              <article className="stat-card">
                <span>Open</span>
                <strong>{formatPrice(snapshot?.quote.open ?? null)}</strong>
              </article>
              <article className="stat-card">
                <span>High</span>
                <strong>{formatPrice(snapshot?.quote.high ?? null)}</strong>
              </article>
              <article className="stat-card">
                <span>Low</span>
                <strong>{formatPrice(snapshot?.quote.low ?? null)}</strong>
              </article>
              <article className="stat-card">
                <span>Volume</span>
                <strong>{formatWhole(snapshot?.quote.volume ?? null)}</strong>
              </article>
            </div>

            <dl className="meta-list">
              <div>
                <dt>Trade Time</dt>
                <dd>{snapshot?.quote.tradeTimeLabel ?? '-'}</dd>
              </div>
              <div>
                <dt>Prev Close</dt>
                <dd>{formatPrice(snapshot?.quote.previousClose ?? null)}</dd>
              </div>
              <div>
                <dt>Trade Value</dt>
                <dd>{formatWhole(snapshot?.quote.tradeValue ?? null)}</dd>
              </div>
              <div>
                <dt>Last Sync</dt>
                <dd>{formatDateTime(snapshot?.fetchedAt ?? null)}</dd>
              </div>
            </dl>
          </section>

          <section className="panel">
            <div className="panel-head compact">
              <div>
                <p className="section-label">Order Book</p>
                <h2>호가</h2>
              </div>
            </div>

            <div className="stock-orderbook-grid">
              <div className="stock-orderbook-column">
                <div className="stock-orderbook-head">
                  <span>Ask</span>
                  <strong>{formatWhole(snapshot?.orderBook.totalAskQuantity ?? null)}</strong>
                </div>
                {(snapshot?.orderBook.asks ?? []).map((level, index) => (
                  <div key={`ask-${index}`} className="stock-orderbook-row">
                    <span>{formatPrice(level.price)}</span>
                    <strong>{formatWhole(level.quantity)}</strong>
                  </div>
                ))}
              </div>

              <div className="stock-orderbook-column">
                <div className="stock-orderbook-head">
                  <span>Bid</span>
                  <strong>{formatWhole(snapshot?.orderBook.totalBidQuantity ?? null)}</strong>
                </div>
                {(snapshot?.orderBook.bids ?? []).map((level, index) => (
                  <div key={`bid-${index}`} className="stock-orderbook-row">
                    <span>{formatPrice(level.price)}</span>
                    <strong>{formatWhole(level.quantity)}</strong>
                  </div>
                ))}
              </div>
            </div>

            <dl className="meta-list">
              <div>
                <dt>Book Time</dt>
                <dd>{snapshot?.orderBook.baseTime ?? '-'}</dd>
              </div>
            </dl>
          </section>
        </aside>
      </section>

      <section className="stock-section-grid">
        {(snapshot?.sections ?? []).map((section) => (
          <article key={section.title} className="panel stock-feed-panel">
            <div className="panel-head compact">
              <div>
                <p className="section-label">Broker Feed</p>
                <h2>{section.title}</h2>
              </div>
            </div>
            <div className="stock-metric-list">
              {section.rows.length === 0 && <p className="muted">표시할 값이 없습니다.</p>}
              {section.rows.map((row) => (
                <div key={`${section.title}-${row.label}`} className="stock-metric-row">
                  <span>{row.label}</span>
                  <strong>{row.value}</strong>
                </div>
              ))}
            </div>
          </article>
        ))}
      </section>
    </div>
  );
}
