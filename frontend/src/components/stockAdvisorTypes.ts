export type StockAdvisorSource = {
  title: string;
  url: string;
  domain: string;
};

export type StockAdvisorResponse = {
  symbol: string;
  riskProfile: string;
  tradeStyle: string;
  generatedAt: string;
  content: string;
  sources: StockAdvisorSource[];
};

export type TradingViewSymbolOption = {
  symbol: string;
  description: string;
  exchange: string;
  type: string;
  country: string;
  directViewOnly: boolean;
  directViewOnlyMessage: string;
};

export type AdvisorMessage = {
  id?: string;
  role: 'assistant' | 'user';
  content: string;
  createdAt: string;
  symbol?: string;
  sources?: StockAdvisorSource[];
};

export type StockAdvisorThreadSummary = {
  threadId: string;
  title: string;
  symbol: string;
  riskProfile: string;
  tradeStyle: string;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
};

export type StockAdvisorThread = {
  threadId: string;
  title: string;
  symbol: string;
  riskProfile: string;
  tradeStyle: string;
  notes: string;
  createdAt: string;
  updatedAt: string;
  messages: AdvisorMessage[];
};
