import { FormEvent, useEffect, useState } from 'react';
import client from '../api/client';
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

const presets = [
  { symbol: '005930', label: '삼성전자' },
  { symbol: '000660', label: 'SK하이닉스' },
  { symbol: '035420', label: 'NAVER' },
  { symbol: '051910', label: 'LG화학' },
  { symbol: '005380', label: '현대차' }
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

function getErrorMessage(error: unknown) {
  if (typeof error === 'object' && error !== null) {
    const response = error as { response?: { data?: { message?: string } } };
    if (response.response?.data?.message) {
      return response.response.data.message;
    }
  }
  return '키움 국내주식 데이터를 불러오지 못했습니다. 앱키, 시크릿키, 허용 IP를 확인하세요.';
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

  return next.slice(-120);
}

export default function KrStock() {
  const theme = useThemeStore((s) => s.theme);
  const [symbolInput, setSymbolInput] = useState('005930');
  const [activeSymbol, setActiveSymbol] = useState('005930');
  const [snapshot, setSnapshot] = useState<KrStockSnapshot | null>(null);
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [mode, setMode] = useState<'intraday' | 'daily'>('intraday');

  useEffect(() => {
    let active = true;
    setLoading(true);
    setMessage('');

    client.get<KrStockSnapshot>(`/kr-stocks/${activeSymbol}`)
      .then((response) => {
        if (!active) {
          return;
        }
        setSnapshot(response.data);
      })
      .catch((error) => {
        if (!active) {
          return;
        }
        setSnapshot(null);
        setMessage(getErrorMessage(error));
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
    const refreshMs = Math.max(2, snapshot?.liveRefreshIntervalSeconds ?? 2) * 1000;

    const timer = window.setInterval(() => {
      client.get<KrStockRealtime>(`/kr-stocks/${activeSymbol}/realtime`)
        .then((response) => {
          setSnapshot((current) => {
            if (!current) {
              return current;
            }
            return {
              ...current,
              fetchedAt: response.data.fetchedAt,
              quote: response.data.quote,
              orderBook: response.data.orderBook,
              intradayCandles: mergeRealtimeCandle(current.intradayCandles, response.data.quote)
            };
          });
        })
        .catch((error) => {
          setMessage(getErrorMessage(error));
        });
    }, refreshMs);

    return () => {
      window.clearInterval(timer);
    };
  }, [activeSymbol, snapshot?.liveRefreshIntervalSeconds]);

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    const normalized = symbolInput.replace(/[^0-9A-Za-z_]/g, '').toUpperCase();
    if (!normalized) {
      setMessage('종목코드를 입력하세요.');
      return;
    }
    setActiveSymbol(normalized);
  };

  const chartData = mode === 'intraday' ? snapshot?.intradayCandles ?? [] : snapshot?.dailyCandles ?? [];

  return (
    <div className="site-frame page-stack">
      <section className="panel stock-hero">
        <div className="stock-topbar">
          <div>
            <p className="section-label">KR Stock</p>
            <h1>키움 국내주식 차트</h1>
            <p className="panel-copy">
              키움 REST API 기준으로 국내 주식 시세, 호가, 분봉, 일봉을 백엔드에서 직접 가져와 표시합니다.
            </p>
          </div>
          <div className="stock-live-pill">
            <strong>{snapshot?.provider ?? 'Kiwoom'}</strong>
            <span>{snapshot ? `${snapshot.liveRefreshIntervalSeconds}초 갱신` : '연결 대기'}</span>
          </div>
        </div>

        <StockMarketWarning message="키움 API는 2026년 3월 23일 기준으로 등록된 허용 IP에서만 요청됩니다. 이 서버 IP가 키움 콘솔에 등록되어 있어야 실제 데이터가 내려옵니다." />

        <form className="stock-search-form" onSubmit={handleSubmit}>
          <input
            value={symbolInput}
            onChange={(event) => setSymbolInput(event.target.value)}
            placeholder="예: 005930"
          />
          <button type="submit" className="button button-primary" disabled={loading}>
            {loading ? 'Loading...' : 'Load KR Stock'}
          </button>
        </form>

        <div className="stock-preset-row">
          {presets.map((preset) => (
            <button
              key={preset.symbol}
              type="button"
              className={`button ${activeSymbol === preset.symbol ? 'button-primary' : 'button-secondary'}`}
              onClick={() => {
                setSymbolInput(preset.symbol);
                setActiveSymbol(preset.symbol);
              }}
            >
              {preset.label}
            </button>
          ))}
        </div>

        {message && <p className="form-message error-text">{message}</p>}
      </section>

      <section className="content-grid columns-2-1 stock-summary-layout">
        <article className="panel stock-chart-panel">
          <div className="panel-head stock-chart-head">
            <div>
              <p className="section-label">Candles</p>
              <h2>{snapshot?.name ?? activeSymbol}</h2>
              <p className="panel-copy">
                {snapshot ? `${snapshot.market} · ${snapshot.symbol}` : '실시간 시세 로딩 중'}
              </p>
            </div>
            <div className="stock-mode-row">
              <button
                type="button"
                className={`button ${mode === 'intraday' ? 'button-primary' : 'button-secondary'}`}
                onClick={() => setMode('intraday')}
              >
                1M
              </button>
              <button
                type="button"
                className={`button ${mode === 'daily' ? 'button-primary' : 'button-secondary'}`}
                onClick={() => setMode('daily')}
              >
                Daily
              </button>
            </div>
          </div>

          {chartData.length > 0 ? (
            <KrStockCandleChart candles={chartData} mode={mode} theme={theme} />
          ) : (
            <div className="stock-chart-empty">
              <p className="muted">표시할 캔들 데이터가 없습니다.</p>
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
