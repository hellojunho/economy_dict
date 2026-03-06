import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import client from '../api/client';
import { getRoleFromToken } from '../utils/auth';

export default function Login() {
  const [login, setLogin] = useState({ userId: '', password: '' });
  const [message, setMessage] = useState('');
  const navigate = useNavigate();

  const onLogin = async () => {
    setMessage('');
    try {
      const res = await client.post('/token', login);
      localStorage.setItem('accessToken', res.data.accessToken);
      const role = getRoleFromToken(res.data.accessToken);
      if (role === 'ADMIN') {
        navigate('/admin');
      } else {
        navigate('/mypage');
      }
    } catch (err) {
      setMessage('로그인에 실패했습니다.');
    }
  };

  return (
    <div className="container">
      <section className="card">
        <h2>로그인</h2>
        <div className="grid">
          <input placeholder="아이디" value={login.userId} onChange={(e) => setLogin({ ...login, userId: e.target.value })} />
          <input type="password" placeholder="비밀번호" value={login.password} onChange={(e) => setLogin({ ...login, password: e.target.value })} />
          <button onClick={onLogin}>로그인</button>
        </div>
        <div className="form-footer">
          <span>로그인 시 </span>
          <Link to="/terms" className="link">이용약관</Link>
          <span>에 동의합니다.</span>
          <span className="divider">|</span>
          <Link to="/signup" className="link">회원가입</Link>
        </div>
      </section>

      {message && (
        <section className="card">
          <p>{message}</p>
        </section>
      )}
    </div>
  );
}
