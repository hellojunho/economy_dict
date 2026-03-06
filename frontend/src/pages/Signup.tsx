import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import client from '../api/client';

export default function Signup() {
  const [signup, setSignup] = useState({ userId: '', username: '', password: '', email: '' });
  const [message, setMessage] = useState('');
  const navigate = useNavigate();

  const onSignup = async () => {
    setMessage('');
    try {
      await client.post('/signup', signup);
      navigate('/signin');
    } catch (err) {
      setMessage('회원가입에 실패했습니다.');
    }
  };

  return (
    <div className="container">
      <section className="card">
        <h2>회원가입</h2>
        <div className="grid">
          <input placeholder="아이디" value={signup.userId} onChange={(e) => setSignup({ ...signup, userId: e.target.value })} />
          <input placeholder="이름" value={signup.username} onChange={(e) => setSignup({ ...signup, username: e.target.value })} />
          <input type="password" placeholder="비밀번호" value={signup.password} onChange={(e) => setSignup({ ...signup, password: e.target.value })} />
          <input placeholder="이메일" value={signup.email} onChange={(e) => setSignup({ ...signup, email: e.target.value })} />
          <button onClick={onSignup}>회원가입</button>
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
