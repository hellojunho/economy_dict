import { FormEvent, KeyboardEvent, useEffect, useRef } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Link } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { useChatStore } from '../stores/chatStore';

function formatAssistantMarkdown(content: string) {
  return content
    .replace(/\r\n/g, '\n')
    .replace(/:\s+(?=\d+\.\s)/g, ':\n\n')
    .replace(/([^\.\n])\.\s+(?=\d+\.\s)/g, '$1.\n\n')
    .replace(/\s+-\s+(?=\*\*)/g, '\n- ')
    .trim();
}

export default function Chat() {
  const composerRef = useRef<HTMLTextAreaElement | null>(null);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const threads = useChatStore((state) => state.threads);
  const activeThread = useChatStore((state) => state.activeThread);
  const draft = useChatStore((state) => state.draft);
  const loading = useChatStore((state) => state.loading);
  const sidebarLoading = useChatStore((state) => state.sidebarLoading);
  const error = useChatStore((state) => state.error);
  const setDraft = useChatStore((state) => state.setDraft);
  const loadThreads = useChatStore((state) => state.loadThreads);
  const selectThread = useChatStore((state) => state.selectThread);
  const createThread = useChatStore((state) => state.createThread);
  const removeThread = useChatStore((state) => state.removeThread);
  const sendMessage = useChatStore((state) => state.sendMessage);
  const reset = useChatStore((state) => state.reset);

  useEffect(() => {
    if (!isAuthenticated) {
      reset();
      return;
    }
    loadThreads();
  }, [isAuthenticated, loadThreads, reset]);

  useEffect(() => {
    const composer = composerRef.current;
    if (!composer) {
      return;
    }

    composer.style.height = '0px';
    composer.style.height = `${Math.max(composer.scrollHeight, 52)}px`;
  }, [draft]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    await sendMessage();
  };

  const handleComposerKeyDown = async (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key !== 'Enter') {
      return;
    }

    if (event.metaKey || event.ctrlKey) {
      return;
    }

    event.preventDefault();
    if (!loading) {
      await sendMessage();
    }
  };

  if (!isAuthenticated) {
    return (
      <div className="site-frame page-stack">
        <section className="panel callout-panel">
          <p className="section-label">AI 채팅</p>
          <h1>로그인 후 이용할 수 있어요</h1>
          <div className="button-row">
            <Link to="/signin" className="button button-primary">로그인</Link>
            <Link to="/signup" className="button button-secondary">가입하기</Link>
          </div>
        </section>
      </div>
    );
  }

  return (
    <div className="site-frame page-stack toss-feed-screen real-chat-screen">
      <section className="toss-feed-layout real-chat-layout">
        <aside className="toss-feed-menu real-chat-menu">
          <button type="button" className="toss-feed-menu-item active">전체 대화</button>
          <button type="button" className="toss-feed-menu-item" onClick={() => createThread()} disabled={loading}>새 대화</button>
        </aside>

        <div className="toss-feed-main real-chat-main">
          <section className="toss-feed-composer-card real-chat-topbar">
            <div className="toss-feed-composer-copy">
              <div className="toss-feed-avatar">AI</div>
              <strong>경제 개념과 용어를 질문해 보세요</strong>
            </div>
            <button type="button" className="button button-primary" onClick={() => createThread()} disabled={loading}>
              새 대화
            </button>
          </section>

          <div className="toss-feed-list real-chat-thread-list">
            {sidebarLoading && <p className="muted">목록을 불러오는 중입니다.</p>}
            {!sidebarLoading && threads.length === 0 && (
              <article className="toss-feed-card">
                <strong>저장된 대화가 없습니다.</strong>
                <p className="muted">새 대화 버튼으로 시작하세요.</p>
              </article>
            )}

            {threads.map((thread) => (
              <article key={thread.threadId} className={`toss-feed-card${activeThread?.threadId === thread.threadId ? ' active' : ''}`}>
                <div className="toss-feed-card-head">
                  <div className="toss-feed-user">
                    <div className="toss-feed-avatar small">{thread.title.slice(0, 1)}</div>
                    <div>
                      <strong>{thread.title}</strong>
                      <span>{new Date(thread.updatedAt).toLocaleString()}</span>
                    </div>
                  </div>
                  <div className="button-row">
                    <button type="button" className="toss-follow-chip" onClick={() => selectThread(thread.threadId)}>
                      열기
                    </button>
                    <button type="button" className="toss-follow-chip muted" onClick={() => removeThread(thread.threadId)}>
                      삭제
                    </button>
                  </div>
                </div>

                {activeThread?.threadId === thread.threadId && activeThread.messages.length > 0 ? (
                  <div className="toss-feed-content">
                    {activeThread.messages.slice(-2).map((item, index) => (
                      <div key={`${item.createdAt}-${index}`} className="toss-feed-message">
                        <span>{item.role === 'assistant' ? 'AI' : 'You'}</span>
                        <div className="toss-feed-markdown">
                          {item.role === 'assistant' ? (
                            <ReactMarkdown remarkPlugins={[remarkGfm]}>{formatAssistantMarkdown(item.content)}</ReactMarkdown>
                          ) : (
                            <p>{item.content}</p>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : null}
              </article>
            ))}
          </div>
        </div>

        <aside className="toss-feed-side real-chat-side">
          <div className="toss-feed-side-head">
            <h2>현재 대화</h2>
          </div>

          <div className="toss-feed-side-panel">
            {activeThread ? (
              <>
                <strong>{activeThread.title}</strong>
                <span>{activeThread.messages.length}개 메시지</span>
                <form className="toss-feed-side-form" onSubmit={handleSubmit}>
                  <textarea
                    ref={composerRef}
                    rows={1}
                    value={draft}
                    onChange={(event) => setDraft(event.target.value)}
                    onKeyDown={handleComposerKeyDown}
                    placeholder="경제 용어 또는 이슈를 입력하세요."
                    className="chat-composer-textarea"
                  />
                  <button type="submit" className="button button-primary" disabled={loading}>
                    {loading ? '전송 중' : '보내기'}
                  </button>
                </form>
                {error && <p className="form-message error-text">{error}</p>}
              </>
            ) : (
              <>
                <strong>대화를 선택하세요</strong>
                <span>왼쪽 목록에서 대화를 열면 이 영역에서 이어서 질문할 수 있습니다.</span>
              </>
            )}
          </div>
        </aside>
      </section>
    </div>
  );
}
