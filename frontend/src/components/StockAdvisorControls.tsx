import { FormEvent, useEffect, useRef, useState } from 'react';
import client from '../api/client';
import { TradingViewSymbolOption } from './stockAdvisorTypes';

type StockAdvisorControlsProps = {
  symbol: string;
  selectedSymbol: TradingViewSymbolOption | null;
  riskProfile: string;
  tradeStyle: string;
  loading: boolean;
  onRiskProfileChange: (value: string) => void;
  onTradeStyleChange: (value: string) => void;
  onSymbolSelect: (option: TradingViewSymbolOption) => void;
  onSymbolInputChange: (value: string) => void;
  onSubmit: (event: FormEvent) => void;
};

const riskProfiles = ['공격적', '균형형', '안정적'];
const tradeStyles = ['단타', '스윙', '장기'];

export default function StockAdvisorControls({
  symbol,
  selectedSymbol,
  riskProfile,
  tradeStyle,
  loading,
  onRiskProfileChange,
  onTradeStyleChange,
  onSymbolSelect,
  onSymbolInputChange,
  onSubmit
}: StockAdvisorControlsProps) {
  const rootRef = useRef<HTMLFormElement | null>(null);
  const [symbolOptions, setSymbolOptions] = useState<TradingViewSymbolOption[]>([]);
  const [showSymbolMenu, setShowSymbolMenu] = useState(false);
  const [symbolLoading, setSymbolLoading] = useState(false);

  useEffect(() => {
    const handlePointerDown = (event: MouseEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) {
        setShowSymbolMenu(false);
      }
    };

    document.addEventListener('mousedown', handlePointerDown);
    return () => document.removeEventListener('mousedown', handlePointerDown);
  }, []);

  useEffect(() => {
    if (!showSymbolMenu) {
      return undefined;
    }

    const timer = window.setTimeout(async () => {
      setSymbolLoading(true);
      try {
        const response = await client.get<TradingViewSymbolOption[]>('/stocks/symbols', {
          params: { query: symbol.trim() }
        });
        setSymbolOptions(response.data);
      } catch (_error) {
        setSymbolOptions([]);
      } finally {
        setSymbolLoading(false);
      }
    }, 180);

    return () => window.clearTimeout(timer);
  }, [showSymbolMenu, symbol]);

  const effectiveSymbol = selectedSymbol?.symbol ?? symbol.trim().toUpperCase();
  const directViewOnly = selectedSymbol?.directViewOnly ?? effectiveSymbol.startsWith('KRX:');
  const directViewOnlyMessage =
    selectedSymbol?.directViewOnlyMessage ||
    (directViewOnly
      ? '이 심볼은 TradingView 임베드보다 TradingView 웹사이트에서 직접 조회해야 할 수 있습니다.'
      : '');

  return (
    <form ref={rootRef} className="stock-advisor-composer" onSubmit={onSubmit}>
      <div className="stock-symbol-combobox">
        <label className="stock-advisor-field">
          <span>TradingView Symbol</span>
          <input
            value={symbol}
            onFocus={() => setShowSymbolMenu(true)}
            onChange={(event) => {
              onSymbolInputChange(event.target.value);
              setShowSymbolMenu(true);
            }}
            placeholder="예: NASDAQ:TSLA"
            autoComplete="off"
          />
        </label>

        {showSymbolMenu && (
          <div className="stock-symbol-menu">
            {symbolLoading && <p className="stock-symbol-menu-empty">심볼을 검색하는 중입니다.</p>}
            {!symbolLoading && symbolOptions.length === 0 && (
              <p className="stock-symbol-menu-empty">검색 결과가 없습니다. 심볼명을 조금 더 구체적으로 입력하세요.</p>
            )}
            {!symbolLoading &&
              symbolOptions.map((option) => (
                <button
                  key={`${option.symbol}-${option.exchange}-${option.type}`}
                  type="button"
                  className="stock-symbol-option"
                  onMouseDown={(event) => {
                    event.preventDefault();
                    setShowSymbolMenu(false);
                    onSymbolSelect(option);
                  }}
                >
                  <strong>{option.symbol}</strong>
                  <span>{option.description}</span>
                  <em>
                    {option.exchange}
                    {option.type ? ` · ${option.type}` : ''}
                  </em>
                </button>
              ))}
          </div>
        )}
      </div>

      {directViewOnly && <p className="stock-symbol-warning">{directViewOnlyMessage}</p>}

      <div className="stock-advisor-select-row">
        <label className="stock-advisor-field">
          <span>투자 성향</span>
          <select value={riskProfile} onChange={(event) => onRiskProfileChange(event.target.value)}>
            {riskProfiles.map((item) => (
              <option key={item} value={item}>
                {item}
              </option>
            ))}
          </select>
        </label>

        <label className="stock-advisor-field">
          <span>매매 스타일</span>
          <select value={tradeStyle} onChange={(event) => onTradeStyleChange(event.target.value)}>
            {tradeStyles.map((item) => (
              <option key={item} value={item}>
                {item}
              </option>
            ))}
          </select>
        </label>
      </div>

      <button type="submit" className="button button-primary" disabled={loading}>
        {loading ? '답변 생성 중...' : '전략 보내기'}
      </button>
    </form>
  );
}
