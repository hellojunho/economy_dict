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
    <div className="auth-layout">
      <section className="auth-panel">
        <p className="section-label">Sign In</p>
        <h1>서비스에 로그인</h1>
        <p className="panel-copy">경제 용어 검색, 데일리 퀴즈, 개인 학습 기록을 이어서 이용할 수 있습니다.</p>

        <form className="form-stack" onSubmit={handleSubmit}>
          <label>
            <span>User ID</span>
            <input value={credentials.userId} onChange={(event) => setCredentials((current) => ({ ...current, userId: event.target.value }))} />
          </label>
          <label>
            <span>Password</span>
            <input type="password" value={credentials.password} onChange={(event) => setCredentials((current) => ({ ...current, password: event.target.value }))} />
          </label>
          <button type="submit" className="button button-primary" disabled={loading}>
            {loading ? 'Signing In...' : 'Sign In'}
          </button>
        </form>

        {message && <p className="form-message error-text">{message}</p>}

        <div className="auth-footer-links">
          <Link to="/terms">이용약관</Link>
          <Link to="/signup">회원가입</Link>
        </div>
      </section>
    </div>
  );
}
