import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Navigate } from 'react-router-dom';
import client from '../api/client';
import { getRoleFromToken, hasValidToken } from '../utils/auth';

type SectionKey = 'overview' | 'users' | 'words' | 'uploads';

type AdminUser = {
  id: number;
  userId: string;
  username: string;
  email?: string | null;
  role: 'GENERAL' | 'ADMIN';
  status: 'ACTIVE' | 'DEACTIVATED';
};

type AdminWord = {
  id: number;
  word: string;
  meaning: string;
  englishWord?: string | null;
  englishMeaning?: string | null;
  source?: string | null;
};

type UploadTask = {
  fileId: string;
  status: string;
  message: string;
  estimatedTime?: string | null;
  progressPercent?: number | null;
  errorLog?: string | null;
};

type DailyStat = {
  targetDate: string;
  newUsersCount: number;
  loginCount: number;
  activeUsersCount: number;
};

type Summary = {
  totalUsers: number;
  activeUsers: number;
  totalWords: number;
  recentUploads: number;
};

const sections: { key: SectionKey; label: string }[] = [
  { key: 'overview', label: 'Overview' },
  { key: 'users', label: 'Users' },
  { key: 'words', label: 'Words' },
  { key: 'uploads', label: 'Uploads' }
];

export default function Admin() {
  const token = localStorage.getItem('accessToken');
  const role = getRoleFromToken(token);
  const [section, setSection] = useState<SectionKey>('overview');
  const [summary, setSummary] = useState<Summary | null>(null);
  const [stats, setStats] = useState<DailyStat[]>([]);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [words, setWords] = useState<AdminWord[]>([]);
  const [uploads, setUploads] = useState<UploadTask[]>([]);
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const [userForm, setUserForm] = useState({ id: 0, userId: '', username: '', password: '', email: '', role: 'GENERAL', status: 'ACTIVE' });
  const [wordForm, setWordForm] = useState({ id: 0, word: '', meaning: '', englishWord: '', englishMeaning: '', source: 'MANUAL' });

  if (!hasValidToken(token) || role !== 'ADMIN') {
    return <Navigate to="/signin" replace />;
  }

  const loadOverview = async () => {
    const [summaryResponse, statsResponse] = await Promise.all([
      client.get<Summary>('/admin/stats/summary'),
      client.get<DailyStat[]>('/admin/stats/daily')
    ]);
    setSummary(summaryResponse.data);
    setStats(statsResponse.data);
  };

  const loadUsers = async () => {
    const response = await client.get<AdminUser[]>('/admin/users');
    setUsers(response.data);
  };

  const loadWords = async () => {
    const response = await client.get<AdminWord[]>('/admin/words');
    setWords(response.data);
  };

  const loadUploads = async () => {
    const response = await client.get<UploadTask[]>('/admin/words/uploads');
    setUploads(response.data);
  };

  const refreshCurrentSection = async () => {
    setLoading(true);
    setMessage('');
    try {
      if (section === 'overview') {
        await loadOverview();
      }
      if (section === 'users') {
        await loadUsers();
      }
      if (section === 'words') {
        await loadWords();
      }
      if (section === 'uploads') {
        await loadUploads();
      }
    } catch {
      setMessage('관리자 데이터를 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    refreshCurrentSection();
  }, [section]);

  useEffect(() => {
    if (section !== 'uploads') {
      return;
    }
    const timer = window.setInterval(() => {
      loadUploads().catch(() => undefined);
    }, 4000);
    return () => window.clearInterval(timer);
  }, [section]);

  const resetUserForm = () => setUserForm({ id: 0, userId: '', username: '', password: '', email: '', role: 'GENERAL', status: 'ACTIVE' });
  const resetWordForm = () => setWordForm({ id: 0, word: '', meaning: '', englishWord: '', englishMeaning: '', source: 'MANUAL' });

  const saveUser = async (event: FormEvent) => {
    event.preventDefault();
    setMessage('');
    if (userForm.id) {
      await client.put(`/admin/users/${userForm.id}`, userForm);
    } else {
      await client.post('/admin/users', userForm);
    }
    resetUserForm();
    await loadUsers();
    setMessage('사용자 정보가 저장되었습니다.');
  };

  const deleteUser = async (id: number) => {
    await client.delete(`/admin/users/${id}`);
    await loadUsers();
    if (userForm.id === id) {
      resetUserForm();
    }
  };

  const saveWord = async (event: FormEvent) => {
    event.preventDefault();
    setMessage('');
    if (wordForm.id) {
      await client.put(`/admin/words/${wordForm.id}`, wordForm);
    } else {
      await client.post('/admin/words', wordForm);
    }
    resetWordForm();
    await loadWords();
    setMessage('단어 정보가 저장되었습니다.');
  };

  const deleteWord = async (id: number) => {
    await client.delete(`/admin/words/${id}`);
    await loadWords();
    if (wordForm.id === id) {
      resetWordForm();
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setMessage('업로드할 PDF 파일을 선택하세요. 최대 20MB까지 허용됩니다.');
      return;
    }
    setUploading(true);
    setMessage('');
    try {
      const formData = new FormData();
      formData.append('file', selectedFile);
      await client.post('/admin/words/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      setSelectedFile(null);
      await loadUploads();
      setMessage('업로드 작업이 등록되었습니다.');
      setSection('uploads');
    } catch (error: any) {
      if (error?.response?.status === 413) {
        setMessage('파일이 너무 큽니다. 최대 20MB까지 업로드할 수 있습니다.');
      } else {
        setMessage('PDF 업로드를 시작하지 못했습니다.');
      }
    } finally {
      setUploading(false);
    }
  };

  const currentUpload = useMemo(() => uploads[0] ?? null, [uploads]);

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar formal-sidebar">
        <div>
          <p className="section-label inverse">Administrative Console</p>
          <h1>Operations</h1>
          <p className="sidebar-copy">사용자, 단어, 업로드 작업, 지표를 단일 콘솔에서 관리합니다.</p>
        </div>
        <nav className="admin-nav-list">
          {sections.map((item) => (
            <button
              key={item.key}
              type="button"
              className={`admin-nav-item ${section === item.key ? 'active' : ''}`}
              onClick={() => setSection(item.key)}
            >
              {item.label}
            </button>
          ))}
        </nav>
      </aside>

      <main className="admin-main formal-admin-main">
        <header className="admin-topbar">
          <div>
            <p className="section-label">{section}</p>
            <h2>{sections.find((item) => item.key === section)?.label}</h2>
          </div>
          <button type="button" className="button button-secondary" onClick={refreshCurrentSection} disabled={loading}>
            {loading ? 'Refreshing...' : 'Refresh'}
          </button>
        </header>

        {message && <p className={`form-message ${message.includes('못') || message.includes('실패') || message.includes('최대') ? 'error-text' : 'success-text'}`}>{message}</p>}

        {section === 'overview' && (
          <div className="page-stack">
            <section className="stat-grid admin-stats-grid">
              <article className="stat-card"><span>Total Users</span><strong>{summary?.totalUsers ?? '-'}</strong><p>전체 등록 사용자</p></article>
              <article className="stat-card"><span>Active Users</span><strong>{summary?.activeUsers ?? '-'}</strong><p>활성 사용자 수</p></article>
              <article className="stat-card"><span>Total Words</span><strong>{summary?.totalWords ?? '-'}</strong><p>등록 경제 용어 수</p></article>
              <article className="stat-card"><span>Recent Uploads</span><strong>{summary?.recentUploads ?? '-'}</strong><p>최근 업로드 작업 수</p></article>
            </section>
            <section className="panel">
              <div className="panel-head compact">
                <div>
                  <p className="section-label">Daily Metrics</p>
                  <h2>일간 지표</h2>
                </div>
              </div>
              <div className="table-wrap">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Date</th>
                      <th>New Users</th>
                      <th>Logins</th>
                      <th>Active Users</th>
                    </tr>
                  </thead>
                  <tbody>
                    {stats.map((item) => (
                      <tr key={item.targetDate}>
                        <td>{item.targetDate}</td>
                        <td>{item.newUsersCount}</td>
                        <td>{item.loginCount}</td>
                        <td>{item.activeUsersCount}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          </div>
        )}

        {section === 'users' && (
          <div className="content-grid columns-1-2">
            <section className="panel">
              <div className="panel-head compact"><div><p className="section-label">CRUD</p><h2>사용자 편집</h2></div></div>
              <form className="form-stack" onSubmit={saveUser}>
                <label><span>User ID</span><input value={userForm.userId} onChange={(e) => setUserForm((current) => ({ ...current, userId: e.target.value }))} /></label>
                <label><span>Username</span><input value={userForm.username} onChange={(e) => setUserForm((current) => ({ ...current, username: e.target.value }))} /></label>
                <label><span>Password</span><input type="password" value={userForm.password} onChange={(e) => setUserForm((current) => ({ ...current, password: e.target.value }))} /></label>
                <label><span>Email</span><input value={userForm.email} onChange={(e) => setUserForm((current) => ({ ...current, email: e.target.value }))} /></label>
                <label><span>Role</span><select value={userForm.role} onChange={(e) => setUserForm((current) => ({ ...current, role: e.target.value as AdminUser['role'] }))}><option value="GENERAL">GENERAL</option><option value="ADMIN">ADMIN</option></select></label>
                <label><span>Status</span><select value={userForm.status} onChange={(e) => setUserForm((current) => ({ ...current, status: e.target.value as AdminUser['status'] }))}><option value="ACTIVE">ACTIVE</option><option value="DEACTIVATED">DEACTIVATED</option></select></label>
                <div className="button-row">
                  <button type="submit" className="button button-primary">Save User</button>
                  <button type="button" className="button button-secondary" onClick={resetUserForm}>Clear</button>
                </div>
              </form>
            </section>
            <section className="panel">
              <div className="panel-head compact"><div><p className="section-label">Records</p><h2>사용자 목록</h2></div></div>
              <div className="table-wrap">
                <table className="data-table">
                  <thead><tr><th>User ID</th><th>Name</th><th>Role</th><th>Status</th><th>Action</th></tr></thead>
                  <tbody>
                    {users.map((user) => (
                      <tr key={user.id}>
                        <td>{user.userId}</td>
                        <td>{user.username}</td>
                        <td>{user.role}</td>
                        <td>{user.status}</td>
                        <td>
                          <div className="table-actions">
                            <button type="button" className="link-button" onClick={() => setUserForm({ ...userForm, id: user.id, userId: user.userId, username: user.username, password: '', email: user.email ?? '', role: user.role, status: user.status })}>Edit</button>
                            <button type="button" className="link-button danger-text" onClick={() => deleteUser(user.id)}>Delete</button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          </div>
        )}

        {section === 'words' && (
          <div className="content-grid columns-1-2">
            <section className="panel">
              <div className="panel-head compact"><div><p className="section-label">CRUD</p><h2>단어 편집</h2></div></div>
              <form className="form-stack" onSubmit={saveWord}>
                <label><span>Word</span><input value={wordForm.word} onChange={(e) => setWordForm((current) => ({ ...current, word: e.target.value }))} /></label>
                <label><span>Meaning</span><textarea rows={5} value={wordForm.meaning} onChange={(e) => setWordForm((current) => ({ ...current, meaning: e.target.value }))} /></label>
                <label><span>English Word</span><input value={wordForm.englishWord} onChange={(e) => setWordForm((current) => ({ ...current, englishWord: e.target.value }))} /></label>
                <label><span>English Meaning</span><textarea rows={4} value={wordForm.englishMeaning} onChange={(e) => setWordForm((current) => ({ ...current, englishMeaning: e.target.value }))} /></label>
                <label><span>Source</span><input value={wordForm.source} onChange={(e) => setWordForm((current) => ({ ...current, source: e.target.value }))} /></label>
                <div className="button-row">
                  <button type="submit" className="button button-primary">Save Word</button>
                  <button type="button" className="button button-secondary" onClick={resetWordForm}>Clear</button>
                </div>
              </form>
            </section>
            <section className="panel">
              <div className="panel-head compact"><div><p className="section-label">Records</p><h2>단어 목록</h2></div></div>
              <div className="table-wrap">
                <table className="data-table">
                  <thead><tr><th>Word</th><th>Meaning</th><th>Source</th><th>Action</th></tr></thead>
                  <tbody>
                    {words.map((word) => (
                      <tr key={word.id}>
                        <td>{word.word}</td>
                        <td>{word.meaning}</td>
                        <td>{word.source ?? '-'}</td>
                        <td>
                          <div className="table-actions">
                            <button type="button" className="link-button" onClick={() => setWordForm({ id: word.id, word: word.word, meaning: word.meaning, englishWord: word.englishWord ?? '', englishMeaning: word.englishMeaning ?? '', source: word.source ?? 'MANUAL' })}>Edit</button>
                            <button type="button" className="link-button danger-text" onClick={() => deleteWord(word.id)}>Delete</button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          </div>
        )}

        {section === 'uploads' && (
          <div className="page-stack">
            <section className="panel">
              <div className="panel-head compact">
                <div>
                  <p className="section-label">AI Import</p>
                  <h2>PDF 업로드</h2>
                </div>
                <span className="data-chip">최대 업로드 용량 20MB</span>
              </div>
              <div className="upload-row">
                <input type="file" accept="application/pdf" onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)} />
                <button type="button" className="button button-primary" onClick={handleUpload} disabled={uploading}>
                  {uploading ? 'Uploading...' : 'Upload PDF'}
                </button>
              </div>
            </section>

            {currentUpload && (
              <section className="panel emphasis-panel">
                <div className="panel-head compact">
                  <div>
                    <p className="section-label">Current Task</p>
                    <h2>{currentUpload.fileId}</h2>
                  </div>
                  <span className="data-chip">{currentUpload.status}</span>
                </div>
                <p>{currentUpload.message}</p>
                <div className="progress-track large">
                  <div className="progress-fill" style={{ width: `${currentUpload.progressPercent ?? 0}%` }} />
                </div>
                <p className="muted">진행률 {Math.round(currentUpload.progressPercent ?? 0)}%</p>
                {currentUpload.errorLog && <pre className="error-log">{currentUpload.errorLog}</pre>}
              </section>
            )}

            <section className="panel">
              <div className="panel-head compact"><div><p className="section-label">History</p><h2>업로드 작업 이력</h2></div></div>
              <div className="table-wrap">
                <table className="data-table">
                  <thead><tr><th>Task ID</th><th>Status</th><th>Progress</th><th>Message</th></tr></thead>
                  <tbody>
                    {uploads.map((task) => (
                      <tr key={task.fileId}>
                        <td>{task.fileId}</td>
                        <td>{task.status}</td>
                        <td>{Math.round(task.progressPercent ?? 0)}%</td>
                        <td>{task.message}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          </div>
        )}
      </main>
    </div>
  );
}
