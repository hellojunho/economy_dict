import { FormEvent, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { useChatStore } from '../stores/chatStore';

export default function Chat() {
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

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    await sendMessage();
  };

  if (!isAuthenticated) {
    return (
      <div className="site-frame page-stack">
        <section className="panel callout-panel">
          <p className="section-label">AI Chat</p>
          <h1>ChatGPT 대화 기능</h1>
          <p className="panel-copy">대화는 사용자별 JSON 파일로 저장되며, 새로고침 후에도 이전 대화 목록과 내용을 유지합니다.</p>
          <div className="button-row">
            <Link to="/signin" className="button button-primary">Sign In</Link>
            <Link to="/signup" className="button button-secondary">Create Account</Link>
          </div>
        </section>
      </div>
    );
  }

  return (
    <div className="site-frame page-stack">
      <section className="panel chat-shell">
        <aside className="chat-sidebar">
          <div className="chat-sidebar-head">
            <div>
              <p className="section-label">Conversation</p>
              <h2>AI Chat</h2>
            </div>
            <button type="button" className="button button-primary" onClick={() => createThread()} disabled={loading}>
              New Chat
            </button>
          </div>
          <div className="chat-thread-list">
            {sidebarLoading && <p className="muted">목록을 불러오는 중입니다.</p>}
            {!sidebarLoading && threads.length === 0 && <p className="muted">생성된 대화가 없습니다.</p>}
            {threads.map((thread) => (
              <div key={thread.threadId} className={`chat-thread-item ${activeThread?.threadId === thread.threadId ? 'active' : ''}`}>
                <button type="button" className="chat-thread-link" onClick={() => selectThread(thread.threadId)}>
                  <strong>{thread.title}</strong>
                  <span>{new Date(thread.updatedAt).toLocaleString()}</span>
                </button>
                <button type="button" className="chat-thread-delete" onClick={() => removeThread(thread.threadId)} aria-label="Delete chat">
                  Delete
                </button>
              </div>
            ))}
          </div>
        </aside>

        <div className="chat-main">
          <div className="chat-main-head">
            <div>
              <p className="section-label">Economics Expert Role</p>
              <h2>{activeThread?.title ?? '새 대화를 시작하세요'}</h2>
            </div>
          </div>

          <div className="chat-message-list">
            {(activeThread?.messages ?? []).length === 0 && (
              <div className="chat-empty-state">
                <p>경제 용어, 정책, 투자 개념을 질문하면 경제 전문가처럼 답변합니다.</p>
                <p className="muted">대화는 `backend/chats` 아래 JSON 파일로 저장됩니다.</p>
              </div>
            )}
            {(activeThread?.messages ?? []).map((item, index) => (
              <article key={`${item.createdAt}-${index}`} className={`chat-bubble ${item.role}`}>
                <span>{item.role === 'assistant' ? 'AI' : 'You'}</span>
                <p>{item.content}</p>
              </article>
            ))}
          </div>

          <form className="chat-composer" onSubmit={handleSubmit}>
            <textarea
              rows={4}
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              placeholder="경제 용어 또는 경제 이슈를 입력하세요."
            />
            <div className="button-row">
              <button type="submit" className="button button-primary" disabled={loading}>
                {loading ? 'Sending...' : 'Send'}
              </button>
            </div>
          </form>
          {error && <p className="form-message error-text">{error}</p>}
        </div>
      </section>
    </div>
  );
}
