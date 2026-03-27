import { FormEvent, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import client from '../api/client';
import { useAuthStore } from '../stores/authStore';
import { getApiErrorMessage } from '../utils/apiError';

export default function Signup() {
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const role = useAuthStore((state) => state.role);
  const [form, setForm] = useState({ userId: '', username: '', password: '', email: '' });
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
      await client.post('/signup', form);
      navigate('/signin');
    } catch (error) {
      setMessage(getApiErrorMessage(error, '회원가입에 실패했습니다. 입력값을 다시 확인하세요.'));
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
          <p className="section-label">New Account</p>
          <h1>학습 계정을 만드세요</h1>
          <p className="panel-copy">기본 정보만 등록하면 용어 학습, 퀴즈, AI 기능을 바로 사용할 수 있습니다.</p>
        </div>

        <div className="auth-library-card">
          <div className="auth-library-switch">
            <Link to="/signin" className="auth-library-switch-item">로그인</Link>
            <button type="button" className="auth-library-switch-item active">회원가입</button>
          </div>

          <form className="form-stack auth-library-form" onSubmit={handleSubmit}>
          <label>
            <span>아이디</span>
            <input value={form.userId} onChange={(event) => setForm((current) => ({ ...current, userId: event.target.value }))} />
          </label>
          <label>
            <span>이름</span>
            <input value={form.username} onChange={(event) => setForm((current) => ({ ...current, username: event.target.value }))} />
          </label>
          <label>
            <span>비밀번호</span>
            <input type="password" value={form.password} onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))} />
          </label>
          <label>
            <span>이메일</span>
            <input value={form.email} onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))} />
          </label>
          <button type="submit" className="button button-primary auth-library-submit" disabled={loading}>
            {loading ? '가입 중' : '계정 만들기'}
          </button>
        </form>

        {message && <p className="form-message error-text">{message}</p>}

        <div className="auth-footer-links auth-library-links">
          <Link to="/terms">이용약관</Link>
          <Link to="/signin">로그인으로 이동</Link>
        </div>
        </div>
      </section>
    </div>
  );
}
