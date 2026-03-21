import { FormEvent, useEffect, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import client from '../api/client';
import { useAuthStore } from '../stores/authStore';
import { getApiErrorMessage } from '../utils/apiError';

type Profile = {
  userId: string;
  username: string;
  email?: string | null;
  role: string;
  status: string;
  learnedWordCount: number;
  correctRate: number;
  activatedAt?: string | null;
};

export default function MyPage() {
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const role = useAuthStore((state) => state.role);
  const clearSession = useAuthStore((state) => state.clearSession);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [form, setForm] = useState({ username: '', email: '', password: '' });
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);

  if (!isAuthenticated) {
    return <Navigate to="/signin" replace />;
  }
  if (role === 'ADMIN') {
    return <Navigate to="/admin" replace />;
  }

  const loadProfile = async () => {
    const response = await client.get<Profile>('/users/me');
    setProfile(response.data);
    setForm({
      username: response.data.username ?? '',
      email: response.data.email ?? '',
      password: ''
    });
  };

  useEffect(() => {
    loadProfile().catch((error) => setMessage(getApiErrorMessage(error, '프로필을 불러오지 못했습니다.')));
  }, []);

  const handleSave = async (event: FormEvent) => {
    event.preventDefault();
    setSaving(true);
    setMessage('');
    try {
      const response = await client.put<Profile>('/users/me', form);
      setProfile(response.data);
      setForm((current) => ({ ...current, password: '' }));
      setMessage('프로필이 저장되었습니다.');
    } catch (error) {
      setMessage(getApiErrorMessage(error, '프로필 저장에 실패했습니다.'));
    } finally {
      setSaving(false);
    }
  };

  const handleLogout = async () => {
    try {
      await client.post('/logout');
    } finally {
      clearSession();
      navigate('/signin');
    }
  };

  const handleWithdraw = async () => {
    const confirmed = window.confirm('정말로 회원 탈퇴를 진행하시겠습니까?');
    if (!confirmed) {
      return;
    }
    await client.delete('/users/me');
    clearSession();
    navigate('/');
  };

  return (
    <div className="site-frame page-stack">
      <section className="panel profile-hero">
        <div>
          <p className="section-label">My Page</p>
          <h1>{profile?.username ?? '사용자'} 님의 학습 현황</h1>
          <p className="panel-copy">학습 통계와 계정 정보를 한 화면에서 관리합니다.</p>
        </div>
        <div className="stat-grid compact-grid">
          <article className="stat-card">
            <span>Learned Words</span>
            <strong>{profile?.learnedWordCount ?? '-'}</strong>
            <p>퀴즈 응답 기준 누적 학습 수</p>
          </article>
          <article className="stat-card">
            <span>Correct Rate</span>
            <strong>{profile ? `${profile.correctRate.toFixed(1)}%` : '-'}</strong>
            <p>정답률 기준 개인 성과</p>
          </article>
        </div>
      </section>

      <section className="content-grid columns-2-1">
        <article className="panel">
          <div className="panel-head compact">
            <div>
              <p className="section-label">Account</p>
              <h2>기본 정보</h2>
            </div>
          </div>
          {profile && (
            <dl className="meta-list roomy">
              <div><dt>User ID</dt><dd>{profile.userId}</dd></div>
              <div><dt>Email</dt><dd>{profile.email || '-'}</dd></div>
              <div><dt>Role</dt><dd>{profile.role}</dd></div>
              <div><dt>Status</dt><dd>{profile.status}</dd></div>
              <div><dt>Activated</dt><dd>{profile.activatedAt ? new Date(profile.activatedAt).toLocaleString() : '-'}</dd></div>
            </dl>
          )}
          <div className="button-row">
            <button type="button" className="button button-secondary" onClick={handleLogout}>Logout</button>
            <button type="button" className="button button-danger" onClick={handleWithdraw}>Withdraw</button>
          </div>
        </article>

        <aside className="panel">
          <div className="panel-head compact">
            <div>
              <p className="section-label">Update</p>
              <h2>프로필 수정</h2>
            </div>
          </div>
          <form className="form-stack" onSubmit={handleSave}>
            <label>
              <span>Username</span>
              <input value={form.username} onChange={(event) => setForm((current) => ({ ...current, username: event.target.value }))} />
            </label>
            <label>
              <span>Email</span>
              <input value={form.email} onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))} />
            </label>
            <label>
              <span>New Password</span>
              <input type="password" value={form.password} onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))} />
            </label>
            <button type="submit" className="button button-primary" disabled={saving}>
              {saving ? 'Saving...' : 'Save Changes'}
            </button>
          </form>
          {message && <p className={`form-message ${message.includes('실패') || message.includes('못') ? 'error-text' : 'success-text'}`}>{message}</p>}
          <p className="muted">데일리 퀴즈는 <Link to="/quiz">Quiz</Link> 화면에서 이어서 풀 수 있고, <Link to="/incorrect-note">오답노트</Link>에서 틀린 문항만 다시 볼 수 있습니다.</p>
        </aside>
      </section>
    </div>
  );
}
