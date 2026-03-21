import { FormEvent, useEffect, useMemo } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { AdminUser, DIRECT_SOURCE_OPTION, SectionKey, useAdminStore } from '../stores/adminStore';

const sections: { key: SectionKey; label: string }[] = [
  { key: 'overview', label: 'Overview' },
  { key: 'users', label: 'Users' },
  { key: 'words', label: 'Words' },
  { key: 'uploads', label: 'Uploads' }
];

export default function Admin() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const role = useAuthStore((state) => state.role);
  const section = useAdminStore((state) => state.section);
  const summary = useAdminStore((state) => state.summary);
  const stats = useAdminStore((state) => state.stats);
  const users = useAdminStore((state) => state.users);
  const words = useAdminStore((state) => state.words);
  const uploads = useAdminStore((state) => state.uploads);
  const sourceOptions = useAdminStore((state) => state.sourceOptions);
  const message = useAdminStore((state) => state.message);
  const loading = useAdminStore((state) => state.loading);
  const uploading = useAdminStore((state) => state.uploading);
  const uploadSourceId = useAdminStore((state) => state.uploadSourceId);
  const uploadSourceName = useAdminStore((state) => state.uploadSourceName);
  const userForm = useAdminStore((state) => state.userForm);
  const wordForm = useAdminStore((state) => state.wordForm);
  const setSection = useAdminStore((state) => state.setSection);
  const setSelectedFile = useAdminStore((state) => state.setSelectedFile);
  const setUploadSourceId = useAdminStore((state) => state.setUploadSourceId);
  const setUploadSourceName = useAdminStore((state) => state.setUploadSourceName);
  const updateUserForm = useAdminStore((state) => state.updateUserForm);
  const updateWordForm = useAdminStore((state) => state.updateWordForm);
  const editUser = useAdminStore((state) => state.editUser);
  const editWord = useAdminStore((state) => state.editWord);
  const resetUserForm = useAdminStore((state) => state.resetUserForm);
  const resetWordForm = useAdminStore((state) => state.resetWordForm);
  const refreshCurrentSection = useAdminStore((state) => state.refreshCurrentSection);
  const loadUploads = useAdminStore((state) => state.loadUploads);
  const saveUser = useAdminStore((state) => state.saveUser);
  const deleteUser = useAdminStore((state) => state.deleteUser);
  const saveWord = useAdminStore((state) => state.saveWord);
  const deleteWord = useAdminStore((state) => state.deleteWord);
  const uploadSelectedFile = useAdminStore((state) => state.uploadSelectedFile);

  if (!isAuthenticated || role !== 'ADMIN') {
    return <Navigate to="/signin" replace />;
  }

  useEffect(() => {
    refreshCurrentSection();
  }, [section, refreshCurrentSection]);

  useEffect(() => {
    if (section !== 'uploads') {
      return;
    }
    const timer = window.setInterval(() => {
      loadUploads();
    }, 4000);
    return () => window.clearInterval(timer);
  }, [section, loadUploads]);

  const handleSaveUser = async (event: FormEvent) => {
    event.preventDefault();
    await saveUser();
  };

  const handleSaveWord = async (event: FormEvent) => {
    event.preventDefault();
    await saveWord();
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
          <button type="button" className="button button-secondary" onClick={() => refreshCurrentSection()} disabled={loading}>
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
              <form className="form-stack" onSubmit={handleSaveUser}>
                <label><span>User ID</span><input value={userForm.userId} onChange={(e) => updateUserForm({ userId: e.target.value })} /></label>
                <label><span>Username</span><input value={userForm.username} onChange={(e) => updateUserForm({ username: e.target.value })} /></label>
                <label><span>Password</span><input type="password" value={userForm.password} onChange={(e) => updateUserForm({ password: e.target.value })} /></label>
                <label><span>Email</span><input value={userForm.email} onChange={(e) => updateUserForm({ email: e.target.value })} /></label>
                <label><span>Role</span><select value={userForm.role} onChange={(e) => updateUserForm({ role: e.target.value as AdminUser['role'] })}><option value="GENERAL">GENERAL</option><option value="ADMIN">ADMIN</option></select></label>
                <label><span>Status</span><select value={userForm.status} onChange={(e) => updateUserForm({ status: e.target.value as AdminUser['status'] })}><option value="ACTIVE">ACTIVE</option><option value="DEACTIVATED">DEACTIVATED</option></select></label>
                <div className="button-row">
                  <button type="submit" className="button button-primary">Save User</button>
                  <button type="button" className="button button-secondary" onClick={() => resetUserForm()}>Clear</button>
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
                            <button type="button" className="link-button" onClick={() => editUser(user)}>Edit</button>
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
              <form className="form-stack" onSubmit={handleSaveWord}>
                <label><span>Word</span><input value={wordForm.word} onChange={(e) => updateWordForm({ word: e.target.value })} /></label>
                <label><span>Meaning</span><textarea rows={5} value={wordForm.meaning} onChange={(e) => updateWordForm({ meaning: e.target.value })} /></label>
                <label><span>English Word</span><input value={wordForm.englishWord} onChange={(e) => updateWordForm({ englishWord: e.target.value })} /></label>
                <label><span>English Meaning</span><textarea rows={4} value={wordForm.englishMeaning} onChange={(e) => updateWordForm({ englishMeaning: e.target.value })} /></label>
                <label>
                  <span>Source</span>
                  <select value={wordForm.sourceId} onChange={(e) => updateWordForm({ sourceId: e.target.value, sourceName: e.target.value === DIRECT_SOURCE_OPTION ? wordForm.sourceName : '' })}>
                    <option value="">선택 안 함</option>
                    {sourceOptions.map((source) => (
                      <option key={source.id} value={String(source.id)}>{source.name}</option>
                    ))}
                    <option value={DIRECT_SOURCE_OPTION}>직접입력</option>
                  </select>
                </label>
                {wordForm.sourceId === DIRECT_SOURCE_OPTION && (
                  <label><span>New Source</span><input value={wordForm.sourceName} onChange={(e) => updateWordForm({ sourceName: e.target.value })} placeholder="예: 한국경제용어 700선" /></label>
                )}
                <div className="button-row">
                  <button type="submit" className="button button-primary">Save Word</button>
                  <button type="button" className="button button-secondary" onClick={() => resetWordForm()}>Clear</button>
                </div>
              </form>
            </section>
            <section className="panel">
              <div className="panel-head compact"><div><p className="section-label">Records</p><h2>단어 목록</h2></div></div>
              <div className="table-wrap">
                <table className="data-table">
                  <thead><tr><th>Word</th><th>English</th><th>Source</th><th>Action</th></tr></thead>
                  <tbody>
                    {words.map((word) => (
                      <tr key={word.id}>
                        <td>{word.word}</td>
                        <td>{word.englishWord ?? '-'}</td>
                        <td>{word.sourceName ?? '-'}</td>
                        <td>
                          <div className="table-actions">
                            <button type="button" className="link-button" onClick={() => editWord(word)}>Edit</button>
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
                  <p className="section-label">Import Files</p>
                  <h2>문서 업로드</h2>
                </div>
              </div>
              <div className="form-stack">
                <label>
                  <span>Supported File</span>
                  <input type="file" accept=".pdf,.txt,.xlsx,.csv,.json,.zip,application/pdf,text/plain,text/csv,application/json,application/zip,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)} />
                </label>
                <label>
                  <span>Source (Optional)</span>
                  <select value={uploadSourceId} onChange={(event) => setUploadSourceId(event.target.value)}>
                    <option value="">선택 안 함</option>
                    {sourceOptions.map((source) => (
                      <option key={source.id} value={String(source.id)}>{source.name}</option>
                    ))}
                    <option value={DIRECT_SOURCE_OPTION}>직접입력</option>
                  </select>
                </label>
                {uploadSourceId === DIRECT_SOURCE_OPTION && (
                  <label>
                    <span>New Source</span>
                    <input value={uploadSourceName} onChange={(event) => setUploadSourceName(event.target.value)} placeholder="예: 한국경제용어 700선" />
                  </label>
                )}
                <p className="muted">최대 업로드 용량은 20MB입니다. 지원 형식은 pdf, txt, xlsx, csv, json, zip 이며, zip은 내부 파일을 순차적으로 파싱합니다.</p>
                <div className="button-row">
                  <button type="button" className="button button-primary" onClick={() => uploadSelectedFile()} disabled={uploading}>
                    {uploading ? 'Uploading...' : 'Start Import'}
                  </button>
                </div>
              </div>
            </section>

            {currentUpload && (
              <section className="panel">
                <div className="panel-head compact"><div><p className="section-label">Current Task</p><h2>현재 작업 상태</h2></div></div>
                <div className="progress-card task-progress-card">
                  <span>{currentUpload.status}</span>
                  <strong>{Math.round(currentUpload.progressPercent ?? 0)}%</strong>
                  <div className="progress-track large">
                    <div className="progress-fill" style={{ width: `${currentUpload.progressPercent ?? 0}%` }} />
                  </div>
                  <p>{currentUpload.message}</p>
                  {currentUpload.estimatedTime && <p className="muted">Estimated: {currentUpload.estimatedTime}</p>}
                  {currentUpload.errorLog && <pre className="error-log-block">{currentUpload.errorLog}</pre>}
                </div>
              </section>
            )}

            <section className="panel">
              <div className="panel-head compact"><div><p className="section-label">Task History</p><h2>업로드 작업 목록</h2></div></div>
              <div className="table-wrap">
                <table className="data-table">
                  <thead><tr><th>Task</th><th>Status</th><th>Progress</th><th>Message</th></tr></thead>
                  <tbody>
                    {uploads.map((upload) => (
                      <tr key={upload.fileId}>
                        <td>{upload.fileId}</td>
                        <td>{upload.status}</td>
                        <td>{Math.round(upload.progressPercent ?? 0)}%</td>
                        <td>{upload.message}</td>
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
