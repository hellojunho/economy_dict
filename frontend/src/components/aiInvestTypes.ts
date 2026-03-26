export type AiInvestSource = {
  title: string;
  url: string;
  domain: string;
};

export type AiInvestMessage = {
  id?: string;
  role: 'user' | 'assistant';
  content: string;
  createdAt: string;
  sources?: AiInvestSource[];
};

export type AiInvestThreadSummary = {
  threadId: string;
  title: string;
  stockName: string;
  market: string;
  riskProfile: string;
  tradeStyle: string;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
};

export type AiInvestThread = {
  threadId: string;
  title: string;
  stockName: string;
  market: string;
  riskProfile: string;
  tradeStyle: string;
  notes: string;
  createdAt: string;
  updatedAt: string;
  messages: AiInvestMessage[];
};
