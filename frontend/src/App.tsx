import { Link, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import Home from './pages/Home';
import Login from './pages/Login';
import Signup from './pages/Signup';
import Terms from './pages/Terms';
import Dictionary from './pages/Dictionary';
import Quiz from './pages/Quiz';
import MyPage from './pages/MyPage';
import Admin from './pages/Admin';
import Chat from './pages/Chat';
import { getRoleFromToken } from './utils/auth';

export default function App() {
  const location = useLocation();
  const navigate = useNavigate();
  const isAuthPage = location.pathname === '/signin' || location.pathname === '/signup';
  const hideTopbar = isAuthPage || location.pathname.startsWith('/admin');
  const token = localStorage.getItem('accessToken');
  const role = getRoleFromToken(token);
  const isAdmin = role === 'ADMIN';
  const hasToken = Boolean(token);

  const onLogout = async () => {
    try {
      await fetch('/api/logout', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${localStorage.getItem('accessToken') ?? ''}`
        }
      });
    } finally {
      localStorage.removeItem('accessToken');
      navigate('/signin');
    }
  };
  return (
    <div>
      {!hideTopbar && (
        <header className="topbar">
          <div className="container topbar-inner">
            <nav className="nav-left">
              <Link to="/">홈</Link>
              <Link to="/dictionary">사전</Link>
              <Link to="/quiz">퀴즈</Link>
              <Link to="/chat">ChatGPT</Link>
              {!isAdmin && <Link to="/mypage">마이페이지</Link>}
              {isAdmin && <Link to="/admin">관리자</Link>}
            </nav>
            <div className="nav-right">
              {!hasToken && (
                <>
                  <Link to="/signin" className="btn-link">Sign In</Link>
                  <Link to="/signup" className="btn-link">Sign Up</Link>
                </>
              )}
              {hasToken && (
                <button className="btn-link" onClick={onLogout}>Logout</button>
              )}
            </div>
          </div>
        </header>
      )}
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/signin" element={<Login />} />
        <Route path="/signup" element={<Signup />} />
        <Route path="/terms" element={<Terms />} />
        <Route path="/dictionary" element={<Dictionary />} />
        <Route path="/quiz" element={<Quiz />} />
        <Route path="/chat" element={<Chat />} />
        <Route path="/mypage" element={<MyPage />} />
        <Route path="/admin" element={<Admin />} />
      </Routes>
    </div>
  );
}
