import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import client from '../api/client';
import { getRoleFromToken } from '../utils/auth';

interface Profile {
  userId: string;
  username: string;
  email?: string;
  status: string;
  role: string;
}

export default function MyPage() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [quizzes, setQuizzes] = useState<string[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    const role = getRoleFromToken(localStorage.getItem('accessToken'));
    if (role === 'ADMIN') {
      navigate('/admin');
      return;
    }
    client.get('/users/me').then((res) => setProfile(res.data));
    client.get('/users/me/quizzes').then((res) => setQuizzes(res.data));
  }, [navigate]);

  return (
    <div className="container">
      <section className="card">
        <h2>마이페이지</h2>
        {profile && (
          <div className="grid">
            <div>아이디: {profile.userId}</div>
            <div>이름: {profile.username}</div>
            <div>이메일: {profile.email}</div>
            <div>상태: {profile.status}</div>
            <div>권한: {profile.role}</div>
          </div>
        )}
      </section>

      <section className="card">
        <h3>푼 퀴즈</h3>
        <ul>
          {quizzes.map((id) => (
            <li key={id}>{id}</li>
          ))}
        </ul>
      </section>
    </div>
  );
}
