import { Link, Route, Routes } from 'react-router-dom';
import Home from './pages/Home';
import Auth from './pages/Auth';
import Dictionary from './pages/Dictionary';
import Quiz from './pages/Quiz';
import MyPage from './pages/MyPage';
import Admin from './pages/Admin';
import Chat from './pages/Chat';

export default function App() {
  return (
    <div>
      <nav className="container nav">
        <Link to="/">홈</Link>
        <Link to="/auth">로그인/회원가입</Link>
        <Link to="/dictionary">사전</Link>
        <Link to="/quiz">퀴즈</Link>
        <Link to="/chat">ChatGPT</Link>
        <Link to="/mypage">마이페이지</Link>
        <Link to="/admin">관리자</Link>
      </nav>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/auth" element={<Auth />} />
        <Route path="/dictionary" element={<Dictionary />} />
        <Route path="/quiz" element={<Quiz />} />
        <Route path="/chat" element={<Chat />} />
        <Route path="/mypage" element={<MyPage />} />
        <Route path="/admin" element={<Admin />} />
      </Routes>
    </div>
  );
}
