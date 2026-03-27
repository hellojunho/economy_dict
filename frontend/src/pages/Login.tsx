import { FormEvent, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import client from '../api/client';
import { useAuthStore } from '../stores/authStore';
import { getApiErrorMessage } from '../utils/apiError';

export default function Login() {
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const role = useAuthStore((state) => state.role);
  const setAccessToken = useAuthStore((state) => state.setAccessToken);
  const [credentials, setCredentials] = useState({ userId: '', password: '' });
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  if (isAuthenticated) {
    return <Navigate to={role === 'ADMIN' ? '/admin/overview' : '/'} replace />;
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setMessage('');
    try {
      const response = await client.post('/token', credentials);
      setAccessToken(response.data.accessToken);
      navigate(response.data.role === 'ADMIN' ? '/admin/overview' : '/mypage');
    } catch (error) {
      setMessage(getApiErrorMessage(error, '로그인에 실패했습니다. 계정 정보를 다시 확인하세요.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-layout auth-library-layout">
      <Link to="/" className="auth-library-brand">
        <span className="auth-library-mark" aria-hidden="true">E</span>
        <span>경제사전</span>
      </Link>

      <section className="auth-library-shell">
        <div className="auth-library-intro">
          <p className="section-label">Member Access</p>
          <h1>학습을 다시 이어가세요</h1>
          <p className="panel-copy">
            저장된 오답노트, AI 대화, 주식 학습 기록을 한 곳에서 이어볼 수 있습니다.
          </p>
        </div>

        <div className="auth-library-card">
          <div className="auth-library-switch">
            <button type="button" className="auth-library-switch-item active">로그인</button>
            <Link to="/signup" className="auth-library-switch-item">회원가입</Link>
          </div>

          <form className="form-stack auth-library-form" onSubmit={handleSubmit}>
            <label>
              <span>아이디</span>
              <input
                placeholder="아이디"
                value={credentials.userId}
                onChange={(event) => setCredentials((current) => ({ ...current, userId: event.target.value }))}
              />
            </label>
            <label>
              <span>비밀번호</span>
              <input
                type="password"
                placeholder="비밀번호"
                value={credentials.password}
                onChange={(event) => setCredentials((current) => ({ ...current, password: event.target.value }))}
              />
            </label>

            <button type="submit" className="button button-primary auth-library-submit" disabled={loading}>
              {loading ? '로그인 중' : '로그인'}
            </button>
          </form>

          {message && <p className="form-message error-text">{message}</p>}

          <div className="auth-footer-links auth-library-links">
            <Link to="/terms">이용약관 보기</Link>
          </div>
        </div>

        <p className="auth-library-footnote">
          아직 경제사전 회원이 아닌가요? <Link to="/signup">가입하기</Link>
        </p>
      </section>
    </div>
  );
}
