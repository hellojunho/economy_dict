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
import TopIncorrectWords from './pages/TopIncorrectWords';
import client from './api/client';
import { useAuthStore } from './stores/authStore';

const publicNav = [
  { to: '/', label: 'Overview' },
  { to: '/words', label: 'Words' },
  { to: '/quiz', label: 'Daily Quiz' },
  { to: '/chat', label: 'AI Chat' }
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
                <Link to="/admin" className="site-nav-link">
                  Admin
                </Link>
              )}
            </nav>

            <div className="site-actions">
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
          <Route path="/search" element={<Navigate to="/words" replace />} />
          <Route path="/dictionary" element={<Navigate to="/words" replace />} />
          <Route path="/quiz" element={<Quiz />} />
          <Route path="/incorrect-note" element={<IncorrectNote />} />
          <Route path="/top-incorrect" element={<TopIncorrectWords />} />
          <Route path="/chat" element={<Chat />} />
          <Route path="/mypage" element={<MyPage />} />
          <Route path="/admin" element={<Admin />} />
          <Route path="/signin" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/terms" element={<Terms />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
