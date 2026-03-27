import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import useTranscriptScroll from '../hooks/useTranscriptScroll';

type TranscriptSource = {
  title?: string;
  url: string;
  domain?: string;
};

type TranscriptMessage = {
  id?: string;
  role: 'assistant' | 'user';
  content: string;
  createdAt: string;
  sources?: TranscriptSource[];
};

type AdvisorTranscriptProps = {
  threadKey?: string | null;
  messages: TranscriptMessage[];
  loading: boolean;
  pendingTitle?: string;
  pendingText?: string;
};

function formatAssistantMarkdown(content: string) {
  return content
    .replace(/\r\n/g, '\n')
    .replace(/:\s+(?=\d+\.\s)/g, ':\n\n')
    .replace(/([^\.\n])\.\s+(?=\d+\.\s)/g, '$1.\n\n')
    .trim();
}

export default function AdvisorTranscript({
  threadKey,
  messages,
  loading,
  pendingTitle = '분석 중',
  pendingText = '최근 공시, 뉴스, 시장 분위기를 수집해서 전략을 정리하고 있습니다.'
}: AdvisorTranscriptProps) {
  const {
    viewportRef,
    handleScroll,
    scrollToBottom,
    showScrollButton
  } = useTranscriptScroll({
    threadKey,
    itemCount: messages.length,
    loading
  });

  return (
    <div className="advisor-transcript-view">
      <div
        ref={viewportRef}
        className="advisor-transcript-scroll stock-advisor-thread"
        onScroll={handleScroll}
      >
        {messages.map((message, index) => (
          <article key={message.id ?? `${message.role}-${message.createdAt}-${index}`} className={`stock-advisor-bubble ${message.role}`}>
            <div className="stock-advisor-bubble-meta">
              <span>
                {new Date(message.createdAt).toLocaleTimeString('ko-KR', {
                  hour: '2-digit',
                  minute: '2-digit'
                })}
              </span>
            </div>

            {message.role === 'assistant' ? (
              <div className="stock-advisor-markdown">
                <ReactMarkdown remarkPlugins={[remarkGfm]}>
                  {formatAssistantMarkdown(message.content)}
                </ReactMarkdown>
              </div>
            ) : (
              <p className="stock-advisor-user-copy">{message.content}</p>
            )}

            {message.sources && message.sources.length > 0 && (
              <div className="stock-advisor-sources">
                {message.sources.map((source) => (
                  <a
                    key={source.url}
                    href={source.url}
                    target="_blank"
                    rel="noreferrer"
                    className="stock-advisor-source-item"
                  >
                    <span>{source.domain || 'source'}</span>
                    <strong>{source.title || source.url}</strong>
                  </a>
                ))}
              </div>
            )}
          </article>
        ))}

        {loading && (
          <article className="stock-advisor-bubble assistant pending">
            <div className="stock-advisor-bubble-meta">
              <strong>{pendingTitle}</strong>
            </div>
            <p className="stock-advisor-user-copy">{pendingText}</p>
          </article>
        )}
      </div>

      {showScrollButton && (
        <button type="button" className="stock-advisor-scroll-bottom" onClick={() => scrollToBottom()}>
          맨 아래로
        </button>
      )}
    </div>
  );
}
