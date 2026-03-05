import { useEffect, useState } from 'react';
import client from '../api/client';

interface UserProfile {
  userId: string;
  username: string;
  role: string;
  status: string;
}

export default function Admin() {
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [selectedUser, setSelectedUser] = useState<string>('');
  const [quizzes, setQuizzes] = useState<string[]>([]);

  useEffect(() => {
    client.get('/admin/users').then((res) => setUsers(res.data));
  }, []);

  const loadQuizzes = async (userId: string) => {
    setSelectedUser(userId);
    const res = await client.get(`/admin/users/${userId}/quizzes`);
    setQuizzes(res.data);
  };

  return (
    <div className="container grid grid-2">
      <section className="card">
        <h2>사용자 관리</h2>
        <div className="grid">
          {users.map((user) => (
            <button key={user.userId} onClick={() => loadQuizzes(user.userId)}>
              {user.userId} ({user.role}) - {user.status}
            </button>
          ))}
        </div>
      </section>

      <section className="card">
        <h2>사용자 퀴즈</h2>
        {selectedUser && <p>{selectedUser}의 푼 퀴즈</p>}
        <ul>
          {quizzes.map((quizId) => (
            <li key={quizId}>{quizId}</li>
          ))}
        </ul>
      </section>
    </div>
  );
}
