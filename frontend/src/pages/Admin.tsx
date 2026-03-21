import { FormEvent, useEffect, useMemo } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { AdminQuizQuestion, AdminUser, DIRECT_SOURCE_OPTION, SectionKey, useAdminStore } from '../stores/adminStore';

const sections: { key: SectionKey; label: string }[] = [
  { key: 'overview', label: 'Overview' },
  { key: 'users', label: 'Users' },
  { key: 'words', label: 'Words' },
  { key: 'uploads', label: 'Uploads' },
  { key: 'quizzes', label: 'Quizzes' }
];

const sectionPathMap: Record<SectionKey, string> = {
  overview: '/admin/overview',
  users: '/admin/users',
  words: '/admin/words',
  uploads: '/admin/uploads',
  quizzes: '/admin/quizzes'
};

function resolveSection(pathname: string): SectionKey {
  const matched = sections.find((item) => pathname === sectionPathMap[item.key]);
  return matched?.key ?? 'overview';
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return '-';
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }

  return parsed.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function renderQuestionRate(question: AdminQuizQuestion) {
  return `${Math.round(question.correctRate * 100)}%`;
}

export default function Admin() {
  const location = useLocation();
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const role = useAuthStore((state) => state.role);
  const summary = useAdminStore((state) => state.summary);
  const stats = useAdminStore((state) => state.stats);
  const users = useAdminStore((state) => state.users);
  const words = useAdminStore((state) => state.words);
  const quizzes = useAdminStore((state) => state.quizzes);
  const selectedQuiz = useAdminStore((state) => state.selectedQuiz);
  const wordListResponse = useAdminStore((state) => state.wordListResponse);
  const wordPage = useAdminStore((state) => state.wordPage);
  const uploads = useAdminStore((state) => state.uploads);
  const sourceOptions = useAdminStore((state) => state.sourceOptions);
  const message = useAdminStore((state) => state.message);
  const loading = useAdminStore((state) => state.loading);
  const uploading = useAdminStore((state) => state.uploading);
  const translatingWords = useAdminStore((state) => state.translatingWords);
  const generatingQuiz = useAdminStore((state) => state.generatingQuiz);
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
  const changeWordPage = useAdminStore((state) => state.changeWordPage);
  const loadUploads = useAdminStore((state) => state.loadUploads);
  const saveUser = useAdminStore((state) => state.saveUser);
  const deleteUser = useAdminStore((state) => state.deleteUser);
  const saveWord = useAdminStore((state) => state.saveWord);
  const deleteWord = useAdminStore((state) => state.deleteWord);
  const translateWordsToEnglish = useAdminStore((state) => state.translateWordsToEnglish);
  const uploadSelectedFile = useAdminStore((state) => state.uploadSelectedFile);
  const selectQuiz = useAdminStore((state) => state.selectQuiz);
  const generateQuiz = useAdminStore((state) => state.generateQuiz);
  const currentSection = useMemo(() => resolveSection(location.pathname), [location.pathname]);

  if (!isAuthenticated || role !== 'ADMIN') {
    return <Navigate to="/signin" replace />;
  }

  useEffect(() => {
    setSection(currentSection);
    refreshCurrentSection(currentSection);
  }, [currentSection, refreshCurrentSection, setSection]);

  useEffect(() => {
    if (currentSection !== 'uploads') {
      return;
    }
    const timer = window.setInterval(() => {
      loadUploads();
    }, 4000);
    return () => window.clearInterval(timer);
  }, [currentSection, loadUploads]);

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
              className={`admin-nav-item ${currentSection === item.key ? 'active' : ''}`}
              onClick={() => navigate(sectionPathMap[item.key])}
            >
              {item.label}
            </button>
          ))}
        </nav>
      </aside>

      <main className="admin-main formal-admin-main">
        <header className="admin-topbar">
          <div>
            <p className="section-label">{currentSection}</p>
            <h2>{sections.find((item) => item.key === currentSection)?.label}</h2>
          </div>
          <button type="button" className="button button-secondary" onClick={() => refreshCurrentSection(currentSection)} disabled={loading}>
            {loading ? 'Refreshing...' : 'Refresh'}
          </button>
        </header>

        {message && <p className={`form-message ${message.includes('못') || message.includes('실패') || message.includes('최대') ? 'error-text' : 'success-text'}`}>{message}</p>}

        {currentSection === 'overview' && (
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

        {currentSection === 'users' && (
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

        {currentSection === 'words' && (
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
              <div className="panel-head compact">
                <div>
                  <p className="section-label">Records</p>
                  <h2>단어 목록</h2>
                  <p className="panel-copy">Words Dashboard는 페이지당 10개씩 표시되며, 전체 용어를 한 번에 영문화할 수 있습니다.</p>
                </div>
                <div className="admin-actions">
                  <button type="button" className="button button-secondary" onClick={() => translateWordsToEnglish()} disabled={translatingWords}>
                    {translatingWords ? 'Translating...' : 'To English'}
                  </button>
                </div>
              </div>
              <div className="table-wrap">
                <table className="data-table">
                  <thead><tr><th>Word</th><th>English</th><th>Source</th><th>Action</th></tr></thead>
                  <tbody>
                    {words.length > 0 ? (
                      words.map((word) => (
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
                      ))
                    ) : (
                      <tr>
                        <td colSpan={4} className="table-empty">등록된 단어가 없습니다.</td>
                      </tr>
                    )}
                  </tbody>
                </table>
                <div className="pager-row">
                  <p className="pager-meta">
                    Page {(wordListResponse?.page ?? 0) + 1} / {Math.max(wordListResponse?.totalPages ?? 1, 1)} · Total {wordListResponse?.totalElements ?? 0}
                  </p>
                  <div className="button-row">
                    <button type="button" className="button button-secondary" onClick={() => changeWordPage(wordPage - 1)} disabled={loading || wordPage <= 0}>
                      Previous
                    </button>
                    <button
                      type="button"
                      className="button button-secondary"
                      onClick={() => changeWordPage(wordPage + 1)}
                      disabled={loading || !wordListResponse || wordPage + 1 >= wordListResponse.totalPages}
                    >
                      Next
                    </button>
                  </div>
                </div>
              </div>
            </section>
          </div>
        )}

        {currentSection === 'uploads' && (
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
                  <p className="muted">File: {currentUpload.originalFileName || '-'}</p>
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
                  <thead><tr><th>Task</th><th>File Name</th><th>Status</th><th>Progress</th><th>Message</th></tr></thead>
                  <tbody>
                    {uploads.map((upload) => (
                      <tr key={upload.fileId}>
                        <td>{upload.fileId}</td>
                        <td>{upload.originalFileName || '-'}</td>
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

        {currentSection === 'quizzes' && (
          <div className="content-grid columns-1-2">
            <section className="panel">
              <div className="panel-head compact">
                <div>
                  <p className="section-label">AI Quiz Builder</p>
                  <h2>퀴즈 생성</h2>
                  <p className="panel-copy">Words 테이블의 용어와 뜻을 바탕으로 AI가 객관식 경제 퀴즈를 생성합니다. 생성된 퀴즈는 문항별 정답률과 참여 사용자 기준으로 바로 확인할 수 있습니다.</p>
                </div>
                <div className="admin-actions">
                  <button type="button" className="button button-primary" onClick={() => generateQuiz()} disabled={generatingQuiz}>
                    {generatingQuiz ? 'Creating...' : 'Create Quiz'}
                  </button>
                </div>
              </div>
              <div className="table-wrap">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Title</th>
                      <th>Questions</th>
                      <th>Participants</th>
                      <th>Created</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {quizzes.length > 0 ? (
                      quizzes.map((quiz) => (
                        <tr key={quiz.id}>
                          <td>{quiz.title}</td>
                          <td>{quiz.questionCount}</td>
                          <td>{quiz.participantCount}</td>
                          <td>{formatDateTime(quiz.createdAt)}</td>
                          <td>
                            <button type="button" className="link-button" onClick={() => selectQuiz(quiz.id)}>
                              View
                            </button>
                          </td>
                        </tr>
                      ))
                    ) : (
                      <tr>
                        <td colSpan={5} className="table-empty">생성된 퀴즈가 없습니다.</td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </section>

            <section className="panel">
              <div className="panel-head compact">
                <div>
                  <p className="section-label">Quiz Detail</p>
                  <h2>{selectedQuiz ? selectedQuiz.title : '퀴즈를 선택하세요'}</h2>
                  {selectedQuiz && (
                    <p className="panel-copy">
                      Quiz ID {selectedQuiz.quizId} · 문항 {selectedQuiz.questionCount}개 · 참여 사용자 {selectedQuiz.participantCount}명
                    </p>
                  )}
                </div>
              </div>

              {selectedQuiz ? (
                <div className="page-stack">
                  {selectedQuiz.questions.map((question, index) => (
                    <div key={question.id} className="panel">
                      <div className="panel-head compact">
                        <div>
                          <p className="section-label">Question {index + 1}</p>
                          <h3>{question.questionText}</h3>
                        </div>
                      </div>
                      <div className="stat-grid admin-stats-grid">
                        <article className="stat-card">
                          <span>Attempted</span>
                          <strong>{question.attemptedUsers}</strong>
                          <p>문제를 푼 사용자 수</p>
                        </article>
                        <article className="stat-card">
                          <span>Correct</span>
                          <strong>{question.correctUsers}</strong>
                          <p>정답을 맞힌 사용자 수</p>
                        </article>
                        <article className="stat-card">
                          <span>Accuracy</span>
                          <strong>{renderQuestionRate(question)}</strong>
                          <p>정답률</p>
                        </article>
                      </div>
                      <div className="table-wrap">
                        <table className="data-table">
                          <thead>
                            <tr>
                              <th>Order</th>
                              <th>Option</th>
                              <th>Answer</th>
                            </tr>
                          </thead>
                          <tbody>
                            {question.options.map((option) => (
                              <tr key={option.id}>
                                <td>{option.optionOrder}</td>
                                <td>{option.optionText}</td>
                                <td>{option.correct ? 'Correct' : '-'}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                      <div className="content-grid columns-1-2">
                        <div>
                          <p className="section-label">Participants</p>
                          <p className="muted">
                            {question.participants.length > 0 ? question.participants.join(', ') : '아직 응답한 사용자가 없습니다.'}
                          </p>
                        </div>
                        <div>
                          <p className="section-label">Correct Users</p>
                          <p className="muted">
                            {question.correctParticipants.length > 0 ? question.correctParticipants.join(', ') : '아직 정답자가 없습니다.'}
                          </p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="muted">왼쪽 목록에서 퀴즈를 선택하면 문항별 보기, 참여 사용자, 정답률을 확인할 수 있습니다.</p>
              )}
            </section>
          </div>
        )}
      </main>
    </div>
  );
}
