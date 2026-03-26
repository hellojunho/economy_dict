import { useEffect } from 'react';
import { Link, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
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
import { useThemeStore } from './stores/themeStore';
import ThemeToggle from './components/ThemeToggle';

const publicNav = [
  { to: '/', label: 'Overview' },
  { to: '/words', label: 'Words' },
  { to: '/stocks', label: 'Stock' },
  { to: '/kr-stocks', label: 'KR Stock' },
  { to: '/quiz', label: 'Daily Quiz' },
  { to: '/chat', label: 'AI Chat' },
  { to: '/ai-recommend', label: 'AI Recommend' },
  { to: '/ai-invest', label: 'AI Invest' }
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
  const isDark = useThemeStore((state) => state.theme === 'dark');
  const toggleTheme = useThemeStore((state) => state.toggle);

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
              <span className="brand-eyebrow">Economy Dictionary & Quiz</span>
              <strong>Economic Learning Platform</strong>
            </Link>

            <nav className="site-nav">
              {publicNav.map((item) => (
                <Link key={item.to} to={item.to} className="site-nav-link">
                  {item.label}
                </Link>
              ))}
              {isAuthenticated && !isAdmin && (
                <Link to="/mypage" className="site-nav-link">
                  My Page
                </Link>
              )}
              {isAuthenticated && isAdmin && (
                <Link to="/admin/overview" className="site-nav-link">
                  Admin
                </Link>
              )}
            </nav>

            <div className="site-actions">
              <ThemeToggle isDark={isDark} onToggle={toggleTheme} />
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
                <button type="button" className="button button-secondary" onClick={handleLogout}>
                  Logout
                </button>
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
    </div>
  );
}
