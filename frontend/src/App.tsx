import { useEffect } from 'react';
import { Link, NavLink, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import Home from './pages/Home';
import Login from './pages/Login';
import Signup from './pages/Signup';
import Terms from './pages/Terms';
import Search from './pages/Search';
import Quiz from './pages/Quiz';
import IncorrectNote from './pages/IncorrectNote';
import MyPage from './pages/MyPage';
import Admin from './pages/Admin';
import Chat from './pages/Chat';
import AIRecommend from './pages/AIRecommend';
import AIInvest from './pages/AIInvest';
import TopIncorrectWords from './pages/TopIncorrectWords';
import Stock from './pages/Stock';
import KrStock from './pages/KrStock';
import client from './api/client';
import { useAuthStore } from './stores/authStore';

const publicNav = [
  { to: '/', label: '홈' },
  { to: '/words', label: '용어' },
  { to: '/quiz', label: '퀴즈' },
  { to: '/chat', label: 'AI 채팅' },
  { to: '/stocks', label: '주식' }
];

const utilityRail = [
  { to: '/mypage', label: '내 학습', short: '내' },
  { to: '/incorrect-note', label: '오답노트', short: '오' },
  { to: '/ai-recommend', label: 'AI 추천', short: '추' },
  { to: '/stocks', label: '주식', short: '주' }
];

export default function App() {
  const location = useLocation();
  const navigate = useNavigate();
  const isAuthPage = ['/signin', '/signup'].includes(location.pathname);
  const accessToken = useAuthStore((state) => state.accessToken);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const role = useAuthStore((state) => state.role);
  const hydrateAuth = useAuthStore((state) => state.hydrateAuth);
  const clearSession = useAuthStore((state) => state.clearSession);
  const isAdmin = role === 'ADMIN';

  useEffect(() => {
    hydrateAuth();
  }, [hydrateAuth]);

  const handleLogout = async () => {
    try {
      if (accessToken) {
        await client.post('/logout');
      }
    } finally {
      clearSession();
      navigate('/signin');
    }
  };

  return (
    <div className="app-shell">
      {!isAuthPage && (
        <header className="site-header">
          <div className="site-frame site-header-inner">
            <Link to="/" className="brand-mark">
              <span className="brand-badge" aria-hidden="true">E</span>
              <div className="brand-copy">
                <span className="brand-eyebrow">Economy Dictionary</span>
                <strong>경제사전</strong>
              </div>
            </Link>

            <nav className="site-nav">
              {publicNav.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) => `site-nav-link${isActive ? ' active' : ''}`}
                >
                  {item.label}
                </NavLink>
              ))}
              {isAuthenticated && !isAdmin && (
                <NavLink to="/mypage" className={({ isActive }) => `site-nav-link${isActive ? ' active' : ''}`}>내 학습</NavLink>
              )}
              {isAuthenticated && isAdmin && (
                <NavLink to="/admin/overview" className={({ isActive }) => `site-nav-link${isActive ? ' active' : ''}`}>관리</NavLink>
              )}
            </nav>

            <div className="site-actions">
              <Link to="/words" className="site-search-shortcut">
                <span>/</span>
                <strong>용어 검색</strong>
              </Link>
              {!isAuthenticated && (
                <>
                  <Link to="/signin" className="button button-secondary">
                    Sign In
                  </Link>
                  <Link to="/signup" className="button button-primary">
                    Sign Up
                  </Link>
                </>
              )}
              {isAuthenticated && (
                <>
                  <Link to={isAdmin ? '/admin/overview' : '/mypage'} className="button button-secondary">
                    {isAdmin ? 'Admin' : '내 학습'}
                  </Link>
                  <button type="button" className="button button-secondary" onClick={handleLogout}>
                    로그아웃
                  </button>
                </>
              )}
            </div>
          </div>
        </header>
      )}

      <main className={isAuthPage ? 'auth-main' : 'site-main'}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/words" element={<Search />} />
          <Route path="/stocks" element={<Stock />} />
          <Route path="/kr-stocks" element={<KrStock />} />
          <Route path="/search" element={<Navigate to="/words" replace />} />
          <Route path="/dictionary" element={<Navigate to="/words" replace />} />
          <Route path="/quiz" element={<Quiz />} />
          <Route path="/incorrect-note" element={<IncorrectNote />} />
          <Route path="/top-incorrect" element={<TopIncorrectWords />} />
          <Route path="/chat" element={<Chat />} />
          <Route path="/ai-recommend" element={<AIRecommend />} />
          <Route path="/ai-invest" element={<AIInvest />} />
          <Route path="/mypage" element={<MyPage />} />
          <Route path="/admin" element={<Navigate to="/admin/overview" replace />} />
          <Route path="/admin/overview" element={<Admin />} />
          <Route path="/admin/chart" element={<Admin />} />
          <Route path="/admin/users" element={<Admin />} />
          <Route path="/admin/users/:userEntry" element={<Admin />} />
          <Route path="/admin/words" element={<Admin />} />
          <Route path="/admin/uploads" element={<Admin />} />
          <Route path="/admin/quizzes" element={<Admin />} />
          <Route path="/signin" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/terms" element={<Terms />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>

      {!isAuthPage && (
        <aside className="site-utility-rail">
          {utilityRail.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `site-utility-item${isActive ? ' active' : ''}`}
            >
              <span className="site-utility-icon">{item.short}</span>
              <strong>{item.label}</strong>
            </NavLink>
          ))}
        </aside>
      )}
    </div>
  );
}
