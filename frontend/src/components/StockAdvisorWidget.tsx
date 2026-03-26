import { useNavigate } from 'react-router-dom';

type StockAdvisorWidgetProps = {
  symbol: string;
};

export default function StockAdvisorWidget({ symbol }: StockAdvisorWidgetProps) {
  const navigate = useNavigate();

  return (
    <button
      type="button"
      className="stock-advisor-fab"
      onClick={() => navigate(`/ai-recommend?symbol=${encodeURIComponent(symbol)}`)}
    >
      <span>AI</span>
      <strong>전략 상담</strong>
    </button>
  );
}
