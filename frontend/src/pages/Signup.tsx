import { FormEvent, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import client from '../api/client';
import { hasValidToken } from '../utils/auth';

export default function Signup() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ userId: '', username: '', password: '', email: '' });
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  if (hasValidToken(localStorage.getItem('accessToken'))) {
    return <Navigate to="/" replace />;
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setMessage('');
    try {
      await client.post('/signup', form);
      navigate('/signin');
    } catch {
      setMessage('회원가입에 실패했습니다. 입력값을 다시 확인하세요.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-layout">
      <section className="auth-panel">
        <p className="section-label">Sign Up</p>
        <h1>신규 계정 등록</h1>
        <p className="panel-copy">기본 정보 등록 후 바로 학습 기능을 사용할 수 있습니다.</p>

        <form className="form-stack" onSubmit={handleSubmit}>
          <label>
            <span>User ID</span>
            <input value={form.userId} onChange={(event) => setForm((current) => ({ ...current, userId: event.target.value }))} />
          </label>
          <label>
            <span>Username</span>
            <input value={form.username} onChange={(event) => setForm((current) => ({ ...current, username: event.target.value }))} />
          </label>
          <label>
            <span>Password</span>
            <input type="password" value={form.password} onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))} />
          </label>
          <label>
            <span>Email</span>
            <input value={form.email} onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))} />
          </label>
          <button type="submit" className="button button-primary" disabled={loading}>
            {loading ? 'Creating...' : 'Create Account'}
          </button>
        </form>

        {message && <p className="form-message error-text">{message}</p>}

        <div className="auth-footer-links">
          <Link to="/terms">이용약관</Link>
          <Link to="/signin">로그인으로 이동</Link>
        </div>
      </section>
    </div>
  );
}
