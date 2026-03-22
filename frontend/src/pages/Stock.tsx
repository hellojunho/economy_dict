import { FormEvent, useEffect, useState } from 'react';
import StockCandleChart from '../components/StockCandleChart';
import client from '../api/client';

type StockMetric = {
  label: string;
  value: string;
};

type StockSection = {
  title: string;
  rows: StockMetric[];
};

type StockCandle = {
  time: number;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
};

type StockQuote = {
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
  marketCap: number | null;
  high52Week: number | null;
  low52Week: number | null;
  per: number | null;
  pbr: number | null;
  eps: number | null;
  bps: number | null;
  listedShares: number | null;
  tradeTimestamp: number | null;
  tradeTimeLabel: string | null;
  riskLabel: string | null;
};

type OrderBookLevel = {
  price: number;
  quantity: number;
};

type OrderBook = {
  asks: OrderBookLevel[];
  bids: OrderBookLevel[];
  totalAskQuantity: number;
  totalBidQuantity: number;
  expectedPrice: number | null;
  expectedChange: number | null;
  expectedChangeRate: number | null;
};

type StockSnapshot = {
  provider: string;
  symbol: string;
  name: string;
  market: string;
  liveRefreshIntervalSeconds: number;
  fetchedAt: string;
  quote: StockQuote;
  orderBook: OrderBook;
  intradayCandles: StockCandle[];
  dailyCandles: StockCandle[];
  sections: StockSection[];
};

type StockRealtime = {
  symbol: string;
  fetchedAt: string;
  quote: StockQuote;
  orderBook: OrderBook;
};

const presets = [
  { symbol: '005930', label: '삼성전자' },
  { symbol: '000660', label: 'SK하이닉스' },
  { symbol: '035420', label: 'NAVER' },
  { symbol: '005380', label: '현대차' },
  { symbol: '035720', label: '카카오' }
];

function getErrorMessage(error: unknown) {
  if (typeof error === 'object' && error !== null) {
    const maybeResponse = error as { response?: { data?: { message?: string } } };
    if (maybeResponse.response?.data?.message) {
      return maybeResponse.response.data.message;
    }
  }
  return '시세를 불러오지 못했습니다. 종목코드와 KIS Open API 설정을 확인하세요.';
}

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

function formatTimestamp(value: string | null) {
  if (!value) {
    return '-';
  }
  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeStyle: 'medium'
  }).format(new Date(value));
}

function mergeRealtimeCandles(candles: StockCandle[], quote: StockQuote) {
  if (!quote.tradeTimestamp || quote.lastPrice === null) {
    return candles;
  }

  const bucket = Math.floor(quote.tradeTimestamp / 60) * 60;
  const nextCandles = [...candles];
  const lastCandle = nextCandles[nextCandles.length - 1];

  if (!lastCandle) {
    nextCandles.push({
      time: bucket,
      open: quote.lastPrice,
      high: quote.lastPrice,
      low: quote.lastPrice,
      close: quote.lastPrice,
      volume: 0
    });
    return nextCandles;
  }

  if (bucket < lastCandle.time) {
    return nextCandles;
  }

  if (bucket === lastCandle.time) {
    nextCandles[nextCandles.length - 1] = {
      ...lastCandle,
      high: Math.max(lastCandle.high, quote.lastPrice),
      low: Math.min(lastCandle.low, quote.lastPrice),
      close: quote.lastPrice
    };
    return nextCandles;
  }

  nextCandles.push({
    time: bucket,
    open: lastCandle.close,
    high: Math.max(lastCandle.close, quote.lastPrice),
    low: Math.min(lastCandle.close, quote.lastPrice),
    close: quote.lastPrice,
    volume: 0
  });

  return nextCandles.slice(-60);
}

export default function Stock() {
  const [symbolInput, setSymbolInput] = useState('005930');
  const [activeSymbol, setActiveSymbol] = useState('005930');
  const [snapshot, setSnapshot] = useState<StockSnapshot | null>(null);
  const [mode, setMode] = useState<'intraday' | 'daily'>('intraday');
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState('');

  useEffect(() => {
    let active = true;
    setLoading(true);
    setMessage('');

    client
      .get<StockSnapshot>(`/stocks/korea/${activeSymbol}`)
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
      client
        .get<StockRealtime>(`/stocks/korea/${activeSymbol}/realtime`)
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
              intradayCandles: mergeRealtimeCandles(current.intradayCandles, response.data.quote)
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
    const normalized = symbolInput.replace(/[^0-9A-Za-z]/g, '').toUpperCase();
    if (!normalized) {
      setMessage('종목코드를 입력하세요.');
      return;
    }
    setActiveSymbol(normalized);
  };

  const quote = snapshot?.quote ?? null;
  const orderBook = snapshot?.orderBook ?? null;
  const chartData = mode === 'intraday' ? snapshot?.intradayCandles ?? [] : snapshot?.dailyCandles ?? [];

  return (
    <div className="site-frame page-stack">
      <section className="panel stock-hero">
        <div className="stock-topbar">
          <div>
            <p className="section-label">Domestic Stock Terminal</p>
            <h1>실시간 국내 주식 차트</h1>
            <p className="panel-copy">
              한국투자 Open API를 백엔드에서 프록시해 현재가, 호가, 당일분봉, 일봉, 원시 브로커 피드를 한 화면에 묶었습니다.
            </p>
          </div>
          <div className="stock-live-pill">
            <strong>{snapshot?.provider ?? 'KIS'}</strong>
            <span>{snapshot ? `${snapshot.liveRefreshIntervalSeconds}초 갱신` : '연결 대기'}</span>
          </div>
        </div>

        <form className="stock-search-form" onSubmit={handleSubmit}>
          <input
            value={symbolInput}
            onChange={(event) => setSymbolInput(event.target.value)}
            placeholder="예: 005930"
          />
          <button type="submit" className="button button-primary" disabled={loading}>
            {loading ? 'Loading...' : 'Load Stock'}
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
              <p className="section-label">Live Chart</p>
              <h2>{snapshot?.name ?? activeSymbol}</h2>
              <p className="panel-copy">
                {snapshot ? `${snapshot.symbol} · ${snapshot.market}` : '실시간 차트 로딩 중'}
              </p>
            </div>
            <div className="stock-mode-row">
              <button
                type="button"
                className={`button ${mode === 'intraday' ? 'button-primary' : 'button-secondary'}`}
                onClick={() => setMode('intraday')}
              >
                Live 1M
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
            <StockCandleChart candles={chartData} mode={mode} />
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
              <strong>{formatPrice(quote?.lastPrice ?? null)}</strong>
              <span className={(quote?.change ?? 0) >= 0 ? 'positive-text' : 'danger-text'}>
                {formatSigned(quote?.change ?? null)} · {formatPercent(quote?.changeRate ?? null)}
              </span>
            </div>

            <div className="compact-grid stock-mini-grid">
              <article className="stat-card">
                <span>Open</span>
                <strong>{formatPrice(quote?.open ?? null)}</strong>
              </article>
              <article className="stat-card">
                <span>High</span>
                <strong>{formatPrice(quote?.high ?? null)}</strong>
              </article>
              <article className="stat-card">
                <span>Low</span>
                <strong>{formatPrice(quote?.low ?? null)}</strong>
              </article>
              <article className="stat-card">
                <span>Volume</span>
                <strong>{formatWhole(quote?.volume ?? null)}</strong>
              </article>
            </div>

            <dl className="meta-list">
              <div>
                <dt>Trade Time</dt>
                <dd>{quote?.tradeTimeLabel ?? '-'}</dd>
              </div>
              <div>
                <dt>Prev Close</dt>
                <dd>{formatPrice(quote?.previousClose ?? null)}</dd>
              </div>
              <div>
                <dt>Market Cap</dt>
                <dd>{formatWhole(quote?.marketCap ?? null)}</dd>
              </div>
              <div>
                <dt>Last Sync</dt>
                <dd>{formatTimestamp(snapshot?.fetchedAt ?? null)}</dd>
              </div>
              <div>
                <dt>Risk</dt>
                <dd>{quote?.riskLabel ?? '-'}</dd>
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
                  <strong>{formatWhole(orderBook?.totalAskQuantity ?? null)}</strong>
                </div>
                {(orderBook?.asks ?? []).map((level, index) => (
                  <div key={`ask-${index}`} className="stock-orderbook-row">
                    <span>{formatPrice(level.price)}</span>
                    <strong>{formatWhole(level.quantity)}</strong>
                  </div>
                ))}
              </div>

              <div className="stock-orderbook-column">
                <div className="stock-orderbook-head">
                  <span>Bid</span>
                  <strong>{formatWhole(orderBook?.totalBidQuantity ?? null)}</strong>
                </div>
                {(orderBook?.bids ?? []).map((level, index) => (
                  <div key={`bid-${index}`} className="stock-orderbook-row">
                    <span>{formatPrice(level.price)}</span>
                    <strong>{formatWhole(level.quantity)}</strong>
                  </div>
                ))}
              </div>
            </div>

            <div className="stock-expected-grid">
              <div>
                <span>Expected</span>
                <strong>{formatPrice(orderBook?.expectedPrice ?? null)}</strong>
              </div>
              <div>
                <span>Change</span>
                <strong>{formatSigned(orderBook?.expectedChange ?? null)}</strong>
              </div>
              <div>
                <span>Change %</span>
                <strong>{formatPercent(orderBook?.expectedChangeRate ?? null)}</strong>
              </div>
            </div>
          </section>
        </aside>
      </section>

      <section className="stock-section-grid">
        {(snapshot?.sections ?? []).map((section) => (
          <article key={section.title} className="panel stock-feed-panel">
            <div className="panel-head compact">
              <div>
                <p className="section-label">Broker Section</p>
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
