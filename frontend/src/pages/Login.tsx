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
    <div className="auth-layout toss-auth-layout">
      <Link to="/" className="toss-auth-brand">경제사전</Link>

      <section className="toss-auth-shell">
        <h1>경제사전 로그인</h1>

        <div className="toss-auth-card">
          <div className="toss-auth-tabs">
            <button type="button" className="toss-auth-tab active">아이디 로그인</button>
            <Link to="/signup" className="toss-auth-tab toss-auth-tab-link">회원가입</Link>
          </div>

          <form className="form-stack toss-auth-form" onSubmit={handleSubmit}>
            <label>
              <input
                placeholder="아이디"
                value={credentials.userId}
                onChange={(event) => setCredentials((current) => ({ ...current, userId: event.target.value }))}
              />
            </label>
            <label>
              <input
                type="password"
                placeholder="비밀번호"
                value={credentials.password}
                onChange={(event) => setCredentials((current) => ({ ...current, password: event.target.value }))}
              />
            </label>

            <button type="submit" className="button button-primary toss-auth-submit" disabled={loading}>
              {loading ? '로그인 중' : '로그인'}
            </button>
          </form>

          {message && <p className="form-message error-text">{message}</p>}

          <div className="auth-footer-links toss-auth-links">
            <Link to="/terms">이용약관 보기</Link>
          </div>
        </div>

        <p className="toss-auth-bottom-link">
          아직 경제사전 회원이 아닌가요? <Link to="/signup">가입하기</Link>
        </p>
      </section>
    </div>
  );
}
