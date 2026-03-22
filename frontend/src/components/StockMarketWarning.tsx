type StockMarketWarningProps = {
  message: string;
};

export default function StockMarketWarning({ message }: StockMarketWarningProps) {
  return (
    <div className="stock-warning" role="alert">
      <strong>Warning</strong>
      <p>{message}</p>
    </div>
  );
}
