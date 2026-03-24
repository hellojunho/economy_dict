import { FormEvent, useEffect, useState } from 'react';
import client from '../api/client';
import StockAdvisorComposer from './StockAdvisorControls';
import StockAdvisorConversation from './StockAdvisorConversation';
import {
  AdvisorMessage,
  StockAdvisorResponse,
  TradingViewSymbolOption
} from './stockAdvisorTypes';
import { getApiErrorMessage } from '../utils/apiError';

type StockAdvisorWidgetProps = {
  symbol: string;
  onSymbolChange: (symbol: string) => void;
};

function normalizeTradingViewSymbol(value: string) {
  return value.trim().replace(/\s+/g, '').toUpperCase();
}

function createId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
}

function buildUserMessage(symbol: string, riskProfile: string, tradeStyle: string) {
  return [`심볼 ${symbol}`, `성향 ${riskProfile}`, `전략 ${tradeStyle}`].join(' · ');
}

function toLooseSymbolOption(symbol: string): TradingViewSymbolOption {
  return {
    symbol,
    description: symbol,
    exchange: symbol.split(':')[0] ?? '',
    type: 'stock',
    country: '',
    directViewOnly: symbol.startsWith('KRX:'),
    directViewOnlyMessage: symbol.startsWith('KRX:')
      ? '이 심볼은 TradingView 임베드보다 TradingView 웹사이트에서 직접 조회해야 할 수 있습니다.'
      : ''
  };
}

export default function StockAdvisorWidget({ symbol, onSymbolChange }: StockAdvisorWidgetProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [symbolQuery, setSymbolQuery] = useState(symbol);
  const [selectedSymbol, setSelectedSymbol] = useState<TradingViewSymbolOption | null>(null);
  const [riskProfile, setRiskProfile] = useState('균형형');
  const [tradeStyle, setTradeStyle] = useState('스윙');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [messages, setMessages] = useState<AdvisorMessage[]>([
    {
      id: createId(),
      role: 'assistant',
      createdAt: new Date().toISOString(),
      content: [
        '심볼을 검색해서 선택하면 최근 공시, 뉴스, 투자심리, 가격대를 종합한 전략을 정리합니다.',
        '대화처럼 기록이 쌓이므로 이전 분석도 계속 확인할 수 있습니다.'
      ].join('\n\n')
    }
  ]);

  useEffect(() => {
    setSymbolQuery(symbol);
    setSelectedSymbol((current) => (current && current.symbol === symbol ? current : toLooseSymbolOption(symbol)));
  }, [symbol]);

  const handleSymbolSelect = (option: TradingViewSymbolOption) => {
    setSelectedSymbol(option);
    setSymbolQuery(option.symbol);
    onSymbolChange(option.symbol);
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();

    const effectiveSymbol = selectedSymbol?.symbol ?? normalizeTradingViewSymbol(symbolQuery);
    if (!effectiveSymbol) {
      setError('TradingView 심볼을 선택하세요.');
      return;
    }

    const submittedSymbol = normalizeTradingViewSymbol(effectiveSymbol);
    const userMessage: AdvisorMessage = {
      id: createId(),
      role: 'user',
      createdAt: new Date().toISOString(),
      symbol: submittedSymbol,
      content: buildUserMessage(submittedSymbol, riskProfile, tradeStyle)
    };

    setLoading(true);
    setError('');
    setIsOpen(true);
    setMessages((current) => [...current, userMessage]);
    onSymbolChange(submittedSymbol);

    try {
      const response = await client.post<StockAdvisorResponse>('/stocks/advisor', {
        symbol: submittedSymbol,
        riskProfile,
        tradeStyle
      });

      setMessages((current) => [
        ...current,
        {
          id: createId(),
          role: 'assistant',
          createdAt: response.data.generatedAt,
          symbol: response.data.symbol,
          content: response.data.content,
          sources: response.data.sources
        }
      ]);
      setSymbolQuery(response.data.symbol);
      setSelectedSymbol((current) =>
        current && current.symbol === response.data.symbol ? current : toLooseSymbolOption(response.data.symbol)
      );
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, '투자 전략 분석을 불러오지 못했습니다.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <button
        type="button"
        className={`stock-advisor-fab ${isOpen ? 'is-open' : ''}`}
        aria-expanded={isOpen}
        aria-controls="stock-advisor-panel"
        onClick={() => setIsOpen((current) => !current)}
      >
        <span>AI</span>
        <strong>전략 상담</strong>
      </button>

      <aside
        id="stock-advisor-panel"
        className={`stock-advisor-panel ${isOpen ? 'open' : ''}`}
        aria-hidden={!isOpen}
      >
        <div className="stock-advisor-head">
          <div>
            <p className="section-label">Stock Advisor</p>
            <h2>실시간 전략 채팅</h2>
          </div>
          <button type="button" className="button button-secondary" onClick={() => setIsOpen(false)}>
            닫기
          </button>
        </div>

        <div className="stock-advisor-chat">
          <StockAdvisorConversation messages={messages} loading={loading} />
          {error && <p className="form-message error-text">{error}</p>}
          <StockAdvisorComposer
            symbol={symbolQuery}
            selectedSymbol={selectedSymbol}
            riskProfile={riskProfile}
            tradeStyle={tradeStyle}
            loading={loading}
            onRiskProfileChange={setRiskProfile}
            onTradeStyleChange={setTradeStyle}
            onSymbolSelect={handleSymbolSelect}
            onSymbolInputChange={(value) => {
              setSelectedSymbol(null);
              setSymbolQuery(value);
            }}
            onSubmit={handleSubmit}
          />
        </div>
      </aside>
    </>
  );
}
