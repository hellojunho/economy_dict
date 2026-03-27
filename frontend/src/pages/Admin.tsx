import { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import type { Chart as ChartInstance, ChartConfiguration } from 'chart.js';
import { Navigate, useLocation, useNavigate, useParams } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { AdminQuizQuestion, AdminUser, DIRECT_SOURCE_OPTION, SectionKey, Summary, useAdminStore } from '../stores/adminStore';

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

const uploadModelPricing = [
  { value: 'gpt-5.4', label: 'GPT-5.4', standardPrice: '$2.00', batchPrice: '$1.00' },
  { value: 'gpt-5.4-mini', label: 'GPT-5.4 mini (5mini)', standardPrice: '$0.60', batchPrice: '$0.30' },
  { value: 'gpt-5.4-nano', label: 'GPT-5.4 nano', standardPrice: '$0.1625', batchPrice: '$0.08125' },
  { value: 'gpt-4.1', label: 'GPT-4.1', standardPrice: '$1.40', batchPrice: '$0.70' },
  { value: 'gpt-4.1-mini', label: 'GPT-4.1 mini', standardPrice: '$0.28', batchPrice: '$0.14' },
  { value: 'gpt-4.1-nano', label: 'GPT-4.1 nano', standardPrice: '$0.07', batchPrice: '$0.035' }
];

function resolveSection(pathname: string): SectionKey {
  if (pathname.startsWith('/admin/users')) {
    return 'users';
  }
  if (pathname.startsWith('/admin/words')) {
    return 'words';
  }
  if (pathname.startsWith('/admin/quizzes')) {
    return 'quizzes';
  }
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

function formatMetricNumber(value?: number | null) {
  if (value == null) {
    return '-';
  }

  return new Intl.NumberFormat('ko-KR').format(value);
}

const metricPalette = ['#111827', '#374151', '#6b7280', '#d1d5db'];
const metricFill = 'rgba(17, 24, 39, 0.88)';
const secondaryFill = 'rgba(75, 85, 99, 0.88)';
const tertiaryFill = 'rgba(156, 163, 175, 0.88)';

function buildSummaryChartConfig(summary: Summary | null): ChartConfiguration<'bar'> {
  return {
    type: 'bar',
    data: {
      labels: ['Total Users', 'Active Users', 'Total Words', 'Recent Uploads'],
      datasets: [
        {
          data: [
            summary?.totalUsers ?? 0,
            summary?.activeUsers ?? 0,
            summary?.totalWords ?? 0,
            summary?.recentUploads ?? 0
          ],
          backgroundColor: metricPalette,
          borderRadius: 10,
          borderSkipped: false,
          maxBarThickness: 42
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: false
        },
        tooltip: {
          backgroundColor: '#111827',
          padding: 12,
          displayColors: false
        }
      },
      scales: {
        x: {
          grid: {
            display: false
          },
          ticks: {
            color: '#6b7280'
          }
        },
        y: {
          beginAtZero: true,
          grid: {
            color: '#e5e7eb'
          },
          ticks: {
            color: '#6b7280',
            precision: 0
          }
        }
      }
    }
  };
}

function buildTrendChartConfig(stats: ReturnType<typeof useAdminStore.getState>['stats']): ChartConfiguration<'bar'> {
  const recentStats = stats.slice(-7);
  const labels = recentStats.map((item) => item.targetDate);

  return {
    type: 'bar',
    data: {
      labels,
      datasets: [
        {
          label: 'New Users',
          data: recentStats.map((item) => item.newUsersCount),
          backgroundColor: metricFill,
          borderRadius: 8,
          borderSkipped: false,
          maxBarThickness: 28
        },
        {
          label: 'Logins',
          data: recentStats.map((item) => item.loginCount),
          backgroundColor: secondaryFill,
          borderRadius: 8,
          borderSkipped: false,
          maxBarThickness: 28
        },
        {
          label: 'Active Users',
          data: recentStats.map((item) => item.activeUsersCount),
          backgroundColor: tertiaryFill,
          borderRadius: 8,
          borderSkipped: false,
          maxBarThickness: 28
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'bottom',
          labels: {
            color: '#374151',
            usePointStyle: true,
            boxWidth: 10,
            boxHeight: 10
          }
        },
        tooltip: {
          backgroundColor: '#111827',
          padding: 12
        }
      },
      scales: {
        x: {
          stacked: false,
          grid: {
            display: false
          },
          ticks: {
            color: '#6b7280'
          }
        },
        y: {
          beginAtZero: true,
          grid: {
            color: '#e5e7eb'
          },
          ticks: {
            color: '#6b7280',
            precision: 0
          }
        }
      }
    }
  };
}

function buildLatestMixChartConfig(stats: ReturnType<typeof useAdminStore.getState>['stats']): ChartConfiguration<'bar'> {
  const latest = stats[stats.length - 1];

  return {
    type: 'bar',
    data: {
      labels: ['New Users', 'Logins', 'Active Users'],
      datasets: [
        {
          data: latest ? [latest.newUsersCount, latest.loginCount, latest.activeUsersCount] : [0, 0, 0],
          backgroundColor: [metricFill, secondaryFill, tertiaryFill],
          borderRadius: 10,
          borderSkipped: false,
          maxBarThickness: 42
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: false
        },
        tooltip: {
          backgroundColor: '#111827',
          padding: 12
        }
      },
      scales: {
        x: {
          grid: {
            display: false
          },
          ticks: {
            color: '#6b7280'
          }
        },
        y: {
          beginAtZero: true,
          grid: {
            color: '#e5e7eb'
          },
          ticks: {
            color: '#6b7280',
            precision: 0
          }
        }
      }
    }
  };
}

type AdminChartCanvasProps = {
  title: string;
  description: string;
  config: ChartConfiguration<'bar'>;
  emptyMessage: string;
  highlight?: string;
};

function AdminChartCanvas({ title, description, config, emptyMessage, highlight }: AdminChartCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    let chart: ChartInstance | null = null;
    let cancelled = false;

    const renderChart = async () => {
      if (!canvasRef.current) {
        return;
      }

      const { default: ChartJS } = await import('chart.js/auto');
      if (cancelled || !canvasRef.current) {
        return;
      }

      chart = new ChartJS(canvasRef.current, config);
    };

    void renderChart();

    return () => {
      cancelled = true;
      chart?.destroy();
    };
  }, [config]);

  const hasData = config.data.datasets.some((dataset) =>
    dataset.data.some((value) => typeof value === 'number' ? value > 0 : Boolean(value))
  );

  return (
    <section className="panel chart-panel">
      <div className="panel-head compact">
        <div>
          <p className="section-label">Visualization</p>
          <h2>{title}</h2>
          <p className="panel-copy">{description}</p>
        </div>
        {highlight && <p className="chart-highlight">{highlight}</p>}
      </div>
      <div className="chart-canvas-wrap">
        {hasData ? <canvas ref={canvasRef} /> : <p className="chart-empty">{emptyMessage}</p>}
      </div>
    </section>
  );
}

export default function Admin() {
  const location = useLocation();
  const navigate = useNavigate();
  const { userEntry, wordEntry, quizEntry, questionEntry } = useParams();
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
  const fileTypeOptions = useAdminStore((state) => state.fileTypeOptions);
  const message = useAdminStore((state) => state.message);
  const loading = useAdminStore((state) => state.loading);
  const uploading = useAdminStore((state) => state.uploading);
  const applyingUploadModel = useAdminStore((state) => state.applyingUploadModel);
  const translatingWords = useAdminStore((state) => state.translatingWords);
  const generatingQuiz = useAdminStore((state) => state.generatingQuiz);
  const uploadSourceId = useAdminStore((state) => state.uploadSourceId);
  const uploadSourceName = useAdminStore((state) => state.uploadSourceName);
  const uploadModel = useAdminStore((state) => state.uploadModel);
  const uploadModelDraft = useAdminStore((state) => state.uploadModelDraft);
  const userForm = useAdminStore((state) => state.userForm);
  const wordForm = useAdminStore((state) => state.wordForm);
  const setSection = useAdminStore((state) => state.setSection);
  const setSelectedFile = useAdminStore((state) => state.setSelectedFile);
  const setUploadSourceId = useAdminStore((state) => state.setUploadSourceId);
  const setUploadSourceName = useAdminStore((state) => state.setUploadSourceName);
  const setUploadModelDraft = useAdminStore((state) => state.setUploadModelDraft);
  const updateUserForm = useAdminStore((state) => state.updateUserForm);
  const updateWordForm = useAdminStore((state) => state.updateWordForm);
  const editUser = useAdminStore((state) => state.editUser);
  const editWord = useAdminStore((state) => state.editWord);
  const resetUserForm = useAdminStore((state) => state.resetUserForm);
  const resetWordForm = useAdminStore((state) => state.resetWordForm);
  const clearSelectedQuiz = useAdminStore((state) => state.clearSelectedQuiz);
  const refreshCurrentSection = useAdminStore((state) => state.refreshCurrentSection);
  const changeWordPage = useAdminStore((state) => state.changeWordPage);
  const loadUploads = useAdminStore((state) => state.loadUploads);
  const applyUploadModel = useAdminStore((state) => state.applyUploadModel);
  const saveUser = useAdminStore((state) => state.saveUser);
  const deleteUser = useAdminStore((state) => state.deleteUser);
  const loadWord = useAdminStore((state) => state.loadWord);
  const saveWord = useAdminStore((state) => state.saveWord);
  const deleteWord = useAdminStore((state) => state.deleteWord);
  const translateWordsToEnglish = useAdminStore((state) => state.translateWordsToEnglish);
  const uploadSelectedFile = useAdminStore((state) => state.uploadSelectedFile);
  const selectQuiz = useAdminStore((state) => state.selectQuiz);
  const generateQuiz = useAdminStore((state) => state.generateQuiz);
  const [showUploadModelInfo, setShowUploadModelInfo] = useState(false);
  const resolvedUploadModel = uploadModel || uploadModelDraft || 'gpt-4.1-nano';
  const currentSection = useMemo(() => resolveSection(location.pathname), [location.pathname]);
  const summaryChartConfig = useMemo(() => buildSummaryChartConfig(summary), [summary]);
  const trendChartConfig = useMemo(() => buildTrendChartConfig(stats), [stats]);
  const latestMixChartConfig = useMemo(() => buildLatestMixChartConfig(stats), [stats]);
  const latestStat = useMemo(() => stats[stats.length - 1] ?? null, [stats]);
  const editingUser = useMemo(
    () => users.find((user) => String(user.id) === userEntry) ?? null,
    [userEntry, users]
  );
  const editingWord = useMemo(
    () => words.find((word) => String(word.id) === wordEntry) ?? null,
    [wordEntry, words]
  );
  const isNewUserPage = userEntry === 'new';
  const isUserEditPage = currentSection === 'users' && Boolean(userEntry);
  const isNewWordPage = wordEntry === 'new';
  const isWordEditPage = currentSection === 'words' && Boolean(wordEntry);
  const isQuizDetailPage = currentSection === 'quizzes' && Boolean(quizEntry);
  const selectedQuestion = useMemo(
    () => selectedQuiz?.questions.find((question) => String(question.id) === questionEntry) ?? null,
    [questionEntry, selectedQuiz]
  );
  const isQuizQuestionDetailPage = isQuizDetailPage && Boolean(questionEntry);
  const wordsWithoutEnglishCount = useMemo(
    () => words.filter((word) => !word.englishWord || !word.englishWord.trim()).length,
    [words]
  );
  const quizQuestionTotal = useMemo(
    () => quizzes.reduce((total, quiz) => total + quiz.questionCount, 0),
    [quizzes]
  );
  const quizParticipantTotal = useMemo(
    () => quizzes.reduce((total, quiz) => total + quiz.participantCount, 0),
    [quizzes]
  );
  const latestQuiz = useMemo(() => quizzes[0] ?? null, [quizzes]);
  const selectedQuizAverageAccuracy = useMemo(() => {
    if (!selectedQuiz || selectedQuiz.questions.length === 0) {
      return '-';
    }
    const average = selectedQuiz.questions.reduce((total, question) => total + question.correctRate, 0) / selectedQuiz.questions.length;
    return `${Math.round(average * 100)}%`;
  }, [selectedQuiz]);
  const selectedQuizAttemptedTotal = useMemo(
    () => selectedQuiz?.questions.reduce((total, question) => total + question.attemptedUsers, 0) ?? 0,
    [selectedQuiz]
  );

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

  useEffect(() => {
    if (currentSection !== 'users') {
      return;
    }

    if (isNewUserPage) {
      resetUserForm();
      return;
    }

    if (!userEntry || !editingUser) {
      return;
    }

    editUser(editingUser);
  }, [currentSection, editUser, editingUser, isNewUserPage, resetUserForm, userEntry]);

  useEffect(() => {
    if (currentSection !== 'words') {
      return;
    }

    if (isNewWordPage) {
      resetWordForm();
      return;
    }

    if (!wordEntry) {
      return;
    }

    if (editingWord) {
      editWord(editingWord);
      return;
    }

    const wordId = Number(wordEntry);
    if (!Number.isNaN(wordId)) {
      void loadWord(wordId);
    }
  }, [currentSection, editWord, editingWord, isNewWordPage, loadWord, resetWordForm, wordEntry]);

  useEffect(() => {
    if (currentSection !== 'quizzes') {
      return;
    }

    if (!quizEntry) {
      clearSelectedQuiz();
      return;
    }

    const quizId = Number(quizEntry);
    if (!Number.isNaN(quizId)) {
      void selectQuiz(quizId);
    }
  }, [clearSelectedQuiz, currentSection, quizEntry, selectQuiz]);

  const handleSaveUser = async (event: FormEvent) => {
    event.preventDefault();
    const saved = await saveUser();
    if (saved) {
      navigate('/admin/users');
    }
  };

  const handleSaveWord = async (event: FormEvent) => {
    event.preventDefault();
    const saved = await saveWord();
    if (saved) {
      navigate('/admin/words');
    }
  };

  const handleGenerateQuiz = async () => {
    const quiz = await generateQuiz();
    if (quiz) {
      navigate(`/admin/quizzes/${quiz.id}`);
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
              className={`admin-nav-item ${currentSection === item.key ? 'active' : ''}`}
              onClick={() => navigate(sectionPathMap[item.key])}
            >
              {item.label}
            </button>
          ))}
        </nav>
      </aside>

      <main className="admin-main formal-admin-main">
        {!['users', 'words', 'quizzes'].includes(currentSection) && (
          <header className="admin-topbar">
            <div>
              <p className="section-label">{currentSection}</p>
              <h2>{sections.find((item) => item.key === currentSection)?.label}</h2>
            </div>
            <button type="button" className="button button-secondary" onClick={() => refreshCurrentSection(currentSection)} disabled={loading}>
              {loading ? 'Refreshing...' : 'Refresh'}
            </button>
          </header>
        )}

        {message && <p className={`form-message ${message.includes('못') || message.includes('실패') || message.includes('최대') ? 'error-text' : 'success-text'}`}>{message}</p>}

        {currentSection === 'overview' && (
          <div className="page-stack">
            <section className="stat-grid admin-stats-grid">
              <article className="stat-card"><span>Total Users</span><strong>{summary?.totalUsers ?? '-'}</strong><p>전체 등록 사용자</p></article>
              <article className="stat-card"><span>Active Users</span><strong>{summary?.activeUsers ?? '-'}</strong><p>활성 사용자 수</p></article>
              <article className="stat-card"><span>Total Words</span><strong>{summary?.totalWords ?? '-'}</strong><p>등록 경제 용어 수</p></article>
              <article className="stat-card"><span>Recent Uploads</span><strong>{summary?.recentUploads ?? '-'}</strong><p>최근 업로드 작업 수</p></article>
            </section>
            <section className="chart-card-grid">
              <AdminChartCanvas
                title="Overview Snapshot"
                description="Overview 카드 수치를 막대그래프로 비교합니다."
                config={summaryChartConfig}
                emptyMessage="표시할 요약 데이터가 없습니다."
              />
              <AdminChartCanvas
                title="Latest Daily Mix"
                description="가장 최근 날짜 기준 신규 사용자, 로그인, 활성 사용자 수치입니다."
                config={latestMixChartConfig}
                emptyMessage="일간 지표가 아직 없습니다."
                highlight={latestStat ? latestStat.targetDate : 'No Data'}
              />
              <AdminChartCanvas
                title="Daily Activity Trend"
                description="최근 7일 일간 지표를 동일한 크기의 그룹 막대그래프로 시각화합니다."
                config={trendChartConfig}
                emptyMessage="표시할 일간 지표가 없습니다."
                highlight={stats.length > 0 ? `${Math.min(stats.length, 7)} Days` : 'No Data'}
              />
            </section>
          </div>
        )}

        {currentSection === 'users' && !isUserEditPage && (
          <div className="page-stack">
            <section className="panel">
              <div className="panel-head compact">
                <div>
                  <p className="section-label">Records</p>
                  <h2>사용자 목록</h2>
                  <p className="panel-copy">첫 화면에서는 사용자 목록만 표시합니다. 수정은 Edit 화면에서 진행합니다.</p>
                </div>
                <div className="admin-actions">
                  <button type="button" className="button button-secondary" onClick={() => refreshCurrentSection(currentSection)} disabled={loading}>
                    {loading ? 'Refreshing...' : 'Refresh'}
                  </button>
                  <button type="button" className="button button-secondary" onClick={() => navigate('/admin/users/new')}>
                    New User
                  </button>
                </div>
              </div>
              <div className="table-wrap">
                <table className="data-table admin-user-table">
                  <thead><tr><th>User ID</th><th>Name</th><th>Role</th><th>Status</th><th>Action</th></tr></thead>
                  <tbody>
                    {users.length > 0 ? (
                      users.map((user) => (
                        <tr key={user.id}>
                          <td className="user-id-cell">{user.userId}</td>
                          <td>{user.username}</td>
                          <td>{user.role}</td>
                          <td>{user.status}</td>
                          <td className="action-cell">
                            <div className="table-actions admin-table-actions">
                              <button type="button" className="link-button" onClick={() => navigate(`/admin/users/${user.id}`)}>Edit</button>
                              <button type="button" className="link-button danger-text" onClick={() => deleteUser(user.id)}>Delete</button>
                            </div>
                          </td>
                        </tr>
                      ))
                    ) : (
                      <tr>
                        <td colSpan={5} className="table-empty">등록된 사용자가 없습니다.</td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </section>
          </div>
        )}

        {currentSection === 'users' && isUserEditPage && (
          <div className="page-stack">
            <section className="panel admin-detail-panel">
              <div className="panel-head compact">
                <div>
                  <p className="section-label">CRUD</p>
                  <h2>{isNewUserPage ? '사용자 생성' : '사용자 편집'}</h2>
                  <p className="panel-copy">
                    {isNewUserPage
                      ? '새 사용자 정보를 입력한 뒤 저장합니다.'
                      : editingUser
                        ? `${editingUser.userId} 계정 정보를 수정합니다.`
                        : '사용자 정보를 불러오는 중입니다.'}
                  </p>
                </div>
                <div className="admin-actions">
                  <button type="button" className="button button-secondary" onClick={() => navigate('/admin/users')}>
                    Back To List
                  </button>
                </div>
              </div>

              {(isNewUserPage || editingUser) ? (
                <form className="form-stack admin-user-form" onSubmit={handleSaveUser}>
                  <label><span>User ID</span><input value={userForm.userId} onChange={(e) => updateUserForm({ userId: e.target.value })} /></label>
                  <label><span>Username</span><input value={userForm.username} onChange={(e) => updateUserForm({ username: e.target.value })} /></label>
                  <label><span>Password</span><input type="password" value={userForm.password} onChange={(e) => updateUserForm({ password: e.target.value })} placeholder={isNewUserPage ? '' : '변경 시에만 입력'} /></label>
                  <label><span>Email</span><input value={userForm.email} onChange={(e) => updateUserForm({ email: e.target.value })} /></label>
                  <label><span>Role</span><select value={userForm.role} onChange={(e) => updateUserForm({ role: e.target.value as AdminUser['role'] })}><option value="GENERAL">GENERAL</option><option value="ADMIN">ADMIN</option></select></label>
                  <label><span>Status</span><select value={userForm.status} onChange={(e) => updateUserForm({ status: e.target.value as AdminUser['status'] })}><option value="ACTIVE">ACTIVE</option><option value="DEACTIVATED">DEACTIVATED</option></select></label>
                  <div className="button-row">
                    <button type="submit" className="button button-primary">Save User</button>
                    <button type="button" className="button button-secondary" onClick={() => resetUserForm()}>Clear</button>
                  </div>
                </form>
              ) : (
                <p className="muted">해당 사용자를 찾지 못했습니다.</p>
              )}
            </section>
          </div>
        )}

        {currentSection === 'words' && !isWordEditPage && (
          <div className="page-stack">
            <section className="stat-grid admin-stats-grid">
              <article className="stat-card"><span>Total Words</span><strong>{formatMetricNumber(wordListResponse?.totalElements ?? 0)}</strong><p>등록된 전체 단어 수</p></article>
              <article className="stat-card"><span>Current Page</span><strong>{formatMetricNumber(words.length)}</strong><p>현재 페이지에 표시 중인 단어</p></article>
              <article className="stat-card"><span>Missing English</span><strong>{formatMetricNumber(wordsWithoutEnglishCount)}</strong><p>현재 페이지 기준 영문 필드 미완성</p></article>
              <article className="stat-card"><span>Sources</span><strong>{formatMetricNumber(sourceOptions.length)}</strong><p>선택 가능한 출처 수</p></article>
            </section>

            <section className="panel">
              <div className="panel-head compact">
                <div>
                  <p className="section-label">Words Dashboard</p>
                  <h2>단어 목록</h2>
                  <p className="panel-copy">단어를 클릭하면 별도 편집 화면으로 이동합니다. 영문 필드가 비어 있는 용어는 현재 페이지 기준으로 영문화할 수 있습니다.</p>
                </div>
                <div className="admin-actions">
                  <button type="button" className="button button-secondary" onClick={() => translateWordsToEnglish()} disabled={translatingWords}>
                    {translatingWords ? 'Translating...' : 'To English'}
                  </button>
                  <button type="button" className="button button-secondary" onClick={() => navigate('/admin/words/new')}>
                    New Word
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
                          <td>
                            <button type="button" className="link-button" onClick={() => navigate(`/admin/words/${word.id}`)}>
                              {word.word}
                            </button>
                          </td>
                          <td>{word.englishWord ?? '-'}</td>
                          <td>{word.sourceName ?? '-'}</td>
                          <td>
                            <div className="table-actions">
                              <button type="button" className="link-button" onClick={() => navigate(`/admin/words/${word.id}`)}>Edit</button>
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

        {currentSection === 'words' && isWordEditPage && (
          <div className="page-stack">
            <section className="panel admin-detail-panel">
              <div className="panel-head compact">
                <div>
                  <p className="section-label">CRUD</p>
                  <h2>{isNewWordPage ? '단어 생성' : '단어 편집'}</h2>
                  <p className="panel-copy">
                    {isNewWordPage
                      ? '새 단어와 뜻을 입력한 뒤 저장합니다.'
                      : wordForm.id
                        ? `${wordForm.word} 항목을 수정합니다.`
                        : '단어 정보를 불러오는 중입니다.'}
                  </p>
                </div>
                <div className="admin-actions">
                  <button type="button" className="button button-secondary" onClick={() => navigate('/admin/words')}>
                    Back To List
                  </button>
                </div>
              </div>

              {(isNewWordPage || wordForm.id) ? (
                <form className="form-stack" onSubmit={handleSaveWord}>
                  <label><span>Word</span><input value={wordForm.word} onChange={(e) => updateWordForm({ word: e.target.value })} /></label>
                  <label><span>Meaning</span><textarea rows={8} value={wordForm.meaning} onChange={(e) => updateWordForm({ meaning: e.target.value })} /></label>
                  <label><span>English Word</span><input value={wordForm.englishWord} onChange={(e) => updateWordForm({ englishWord: e.target.value })} /></label>
                  <label><span>English Meaning</span><textarea rows={4} value={wordForm.englishMeaning} onChange={(e) => updateWordForm({ englishMeaning: e.target.value })} /></label>
                  <label>
                    <span>File Type</span>
                    <select value={wordForm.fileType} onChange={(e) => updateWordForm({ fileType: e.target.value })}>
                      {fileTypeOptions.map((option) => (
                        <option key={option.code} value={option.code}>{option.displayName}</option>
                      ))}
                    </select>
                  </label>
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
              ) : (
                <p className="muted">해당 단어를 찾지 못했습니다.</p>
              )}
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
                <div className="upload-model-block">
                  <div className="upload-model-head">
                    <label className="upload-model-label">
                      <span className="upload-model-label-row">
                        <span>GPT API Model</span>
                        <button
                          type="button"
                          className={`upload-model-info-button${showUploadModelInfo ? ' active' : ''}`}
                          onClick={() => setShowUploadModelInfo((value) => !value)}
                          aria-expanded={showUploadModelInfo}
                          aria-label="GPT 모델 과금 정보 보기"
                        >
                          i
                        </button>
                      </span>
                      <select
                        className="upload-model-select"
                        value={uploadModelDraft || resolvedUploadModel}
                        onChange={(event) => setUploadModelDraft(event.target.value)}
                      >
                        {uploadModelPricing.map((item) => (
                          <option key={item.value} value={item.value}>{item.label}</option>
                        ))}
                      </select>
                    </label>
                    <div className="upload-model-actions">
                      <button
                        type="button"
                        className="button button-secondary"
                        onClick={() => applyUploadModel()}
                        disabled={applyingUploadModel || !uploadModelDraft || uploadModelDraft === resolvedUploadModel}
                      >
                        {applyingUploadModel ? 'Applying...' : 'Apply'}
                      </button>
                    </div>
                  </div>
                  <p className="muted">현재 적용 모델: {resolvedUploadModel}</p>

                  {showUploadModelInfo && (
                    <div className="upload-model-info-panel">
                      <div className="table-wrap">
                        <table className="data-table upload-pricing-table">
                          <thead>
                            <tr>
                              <th>모델</th>
                              <th>일반 호출</th>
                              <th>Batch API 사용 시</th>
                            </tr>
                          </thead>
                          <tbody>
                            {uploadModelPricing.map((item) => (
                              <tr key={item.value}>
                                <td>{item.label}</td>
                                <td><strong>{item.standardPrice}</strong></td>
                                <td><strong>{item.batchPrice}</strong></td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                      <div className="upload-model-callout">
                        <div>
                          <strong>Batch API란?</strong>
                          <p>
                            Batch API는 요청을 실시간으로 바로 응답받는 대신, 여러 작업을 묶어 비동기로 처리하는 방식입니다.
                            처리 시간이 더 걸릴 수 있지만 단가가 낮아서 대량 업로드나 대량 용어 정리에 유리합니다.
                          </p>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
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

        {currentSection === 'quizzes' && !isQuizDetailPage && (
          <div className="page-stack">
            <section className="stat-grid admin-stats-grid">
              <article className="stat-card"><span>Total Quizzes</span><strong>{formatMetricNumber(quizzes.length)}</strong><p>생성된 전체 퀴즈 수</p></article>
              <article className="stat-card"><span>Total Questions</span><strong>{formatMetricNumber(quizQuestionTotal)}</strong><p>모든 퀴즈의 문항 수 합계</p></article>
              <article className="stat-card"><span>Total Participants</span><strong>{formatMetricNumber(quizParticipantTotal)}</strong><p>퀴즈별 참여 인원 합계</p></article>
              <article className="stat-card"><span>Latest Quiz</span><strong>{latestQuiz ? formatDateTime(latestQuiz.createdAt) : '-'}</strong><p>가장 최근 생성된 퀴즈</p></article>
            </section>

            <section className="panel">
              <div className="panel-head compact">
                <div>
                  <p className="section-label">Quiz Dashboard</p>
                  <h2>퀴즈 목록</h2>
                  <p className="panel-copy">퀴즈를 선택하면 별도 상세 화면에서 문항별 보기, 참여 사용자, 정답률을 확인할 수 있습니다.</p>
                </div>
                <div className="admin-actions">
                  <button type="button" className="button button-primary" onClick={handleGenerateQuiz} disabled={generatingQuiz}>
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
                          <td>
                            <button type="button" className="link-button" onClick={() => navigate(`/admin/quizzes/${quiz.id}`)}>
                              {quiz.title}
                            </button>
                          </td>
                          <td>{quiz.questionCount}</td>
                          <td>{quiz.participantCount}</td>
                          <td>{formatDateTime(quiz.createdAt)}</td>
                          <td>
                            <button type="button" className="link-button" onClick={() => navigate(`/admin/quizzes/${quiz.id}`)}>
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
          </div>
        )}

        {currentSection === 'quizzes' && isQuizDetailPage && (
          <div className="page-stack">
            {selectedQuiz ? (
              isQuizQuestionDetailPage ? (
                selectedQuestion ? (
                  <section className="panel admin-detail-panel">
                    <div className="panel-head compact">
                      <div>
                        <p className="section-label">Question Detail</p>
                        <h2>{selectedQuestion.questionText}</h2>
                        <p className="panel-copy">선택한 문제의 응답 현황과 보기 구성을 확인합니다.</p>
                      </div>
                      <div className="admin-actions">
                        <button
                          type="button"
                          className="button button-secondary"
                          onClick={() => navigate(`/admin/quizzes/${selectedQuiz.id}`)}
                        >
                          Back To Questions
                        </button>
                      </div>
                    </div>
                    <div className="stat-grid admin-stats-grid">
                      <article className="stat-card">
                        <span>Attempted</span>
                        <strong>{selectedQuestion.attemptedUsers}</strong>
                        <p>문제를 푼 사용자 수</p>
                      </article>
                      <article className="stat-card">
                        <span>Correct</span>
                        <strong>{selectedQuestion.correctUsers}</strong>
                        <p>정답을 맞힌 사용자 수</p>
                      </article>
                      <article className="stat-card">
                        <span>Accuracy</span>
                        <strong>{renderQuestionRate(selectedQuestion)}</strong>
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
                          {selectedQuestion.options.map((option) => (
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
                          {selectedQuestion.participants.length > 0 ? selectedQuestion.participants.join(', ') : '아직 응답한 사용자가 없습니다.'}
                        </p>
                      </div>
                      <div>
                        <p className="section-label">Correct Users</p>
                        <p className="muted">
                          {selectedQuestion.correctParticipants.length > 0 ? selectedQuestion.correctParticipants.join(', ') : '아직 정답자가 없습니다.'}
                        </p>
                      </div>
                    </div>
                  </section>
                ) : (
                  <p className="muted">해당 문제를 찾지 못했습니다.</p>
                )
              ) : (
                <section className="panel admin-detail-panel">
                  <div className="panel-head compact">
                    <div>
                      <p className="section-label">Quiz Dashboard</p>
                      <h2>문제 목록</h2>
                      <p className="panel-copy">문제를 클릭하면 개별 문제의 Attempted, Correct, Accuracy와 보기를 확인할 수 있습니다.</p>
                    </div>
                    <div className="admin-actions">
                      <button type="button" className="button button-secondary" onClick={() => navigate('/admin/quizzes')}>
                        Back To List
                      </button>
                    </div>
                  </div>
                  <section className="stat-grid admin-stats-grid">
                    <article className="stat-card">
                      <span>Questions</span>
                      <strong>{selectedQuiz.questionCount}</strong>
                      <p>이 퀴즈에 포함된 전체 문항 수</p>
                    </article>
                    <article className="stat-card">
                      <span>Participants</span>
                      <strong>{selectedQuiz.participantCount}</strong>
                      <p>퀴즈에 참여한 전체 사용자 수</p>
                    </article>
                    <article className="stat-card">
                      <span>Total Attempted</span>
                      <strong>{selectedQuizAttemptedTotal}</strong>
                      <p>문항 단위 응답 수 합계</p>
                    </article>
                    <article className="stat-card">
                      <span>Average Accuracy</span>
                      <strong>{selectedQuizAverageAccuracy}</strong>
                      <p>문항 평균 정답률</p>
                    </article>
                  </section>
                  <div className="table-wrap">
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>No.</th>
                          <th>Question</th>
                          <th>Attempted</th>
                          <th>Correct</th>
                          <th>Accuracy</th>
                          <th>Action</th>
                        </tr>
                      </thead>
                      <tbody>
                        {selectedQuiz.questions.map((question, index) => (
                          <tr key={question.id}>
                            <td>{index + 1}</td>
                            <td>
                              <button
                                type="button"
                                className="link-button"
                                onClick={() => navigate(`/admin/quizzes/${selectedQuiz.id}/questions/${question.id}`)}
                              >
                                {question.questionText}
                              </button>
                            </td>
                            <td>{question.attemptedUsers}</td>
                            <td>{question.correctUsers}</td>
                            <td>{renderQuestionRate(question)}</td>
                            <td>
                              <button
                                type="button"
                                className="link-button"
                                onClick={() => navigate(`/admin/quizzes/${selectedQuiz.id}/questions/${question.id}`)}
                              >
                                View
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </section>
              )
            ) : (
              <p className="muted">해당 퀴즈를 찾지 못했거나 아직 불러오는 중입니다.</p>
            )}
          </div>
        )}
      </main>
    </div>
  );
}
