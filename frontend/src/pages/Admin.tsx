import { useEffect, useMemo, useState } from 'react';
import client from '../api/client';

type TabKey = 'users' | 'dictionary' | 'quizzes' | 'questions' | 'options';

type AdminUser = {
  id: number;
  userId: string;
  username: string;
  email?: string;
  profilePicture?: string;
  role: 'GENERAL' | 'ADMIN';
  status: 'ACTIVE' | 'DEACTIVATED';
};

type DictionaryEntry = {
  id: number;
  word: string;
  meaning: string;
  englishWord?: string;
  englishMeaning?: string;
};

type Quiz = {
  id: number;
  quizId: string;
  title: string;
};

type Question = {
  id: number;
  quizId: number;
  questionText: string;
};

type Option = {
  id: number;
  questionId: number;
  optionText: string;
  optionOrder: number;
  correct: boolean;
};

const tabs: { key: TabKey; label: string }[] = [
  { key: 'users', label: 'Users' },
  { key: 'dictionary', label: 'Dictionary' },
  { key: 'quizzes', label: 'Quiz' },
  { key: 'questions', label: 'Quiz Question' },
  { key: 'options', label: 'Quiz Option' }
];

export default function Admin() {
  const [activeTab, setActiveTab] = useState<TabKey>('users');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [users, setUsers] = useState<AdminUser[]>([]);
  const [userForm, setUserForm] = useState({
    id: 0,
    userId: '',
    username: '',
    password: '',
    email: '',
    role: 'GENERAL' as AdminUser['role'],
    status: 'ACTIVE' as AdminUser['status']
  });

  const [dictionary, setDictionary] = useState<DictionaryEntry[]>([]);
  const [dictionaryForm, setDictionaryForm] = useState({
    id: 0,
    word: '',
    meaning: '',
    englishWord: '',
    englishMeaning: ''
  });

  const [quizzes, setQuizzes] = useState<Quiz[]>([]);
  const [quizForm, setQuizForm] = useState({
    id: 0,
    title: ''
  });

  const [questions, setQuestions] = useState<Question[]>([]);
  const [questionForm, setQuestionForm] = useState({
    id: 0,
    quizId: 0,
    questionText: ''
  });

  const [options, setOptions] = useState<Option[]>([]);
  const [optionForm, setOptionForm] = useState({
    id: 0,
    questionId: 0,
    optionText: '',
    optionOrder: 1,
    correct: false
  });

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      if (activeTab === 'users') {
        const res = await client.get('/admin/users');
        setUsers(res.data);
      }
      if (activeTab === 'dictionary') {
        const res = await client.get('/admin/dictionary');
        setDictionary(res.data);
      }
      if (activeTab === 'quizzes') {
        const res = await client.get('/admin/quizzes');
        setQuizzes(res.data);
      }
      if (activeTab === 'questions') {
        const res = await client.get('/admin/questions');
        setQuestions(res.data);
      }
      if (activeTab === 'options') {
        const res = await client.get('/admin/options');
        setOptions(res.data);
      }
    } catch (err) {
      setError('데이터를 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [activeTab]);

  const headerTitle = useMemo(() => tabs.find((t) => t.key === activeTab)?.label ?? '', [activeTab]);

  const resetForms = () => {
    setUserForm({ id: 0, userId: '', username: '', password: '', email: '', role: 'GENERAL', status: 'ACTIVE' });
    setDictionaryForm({ id: 0, word: '', meaning: '', englishWord: '', englishMeaning: '' });
    setQuizForm({ id: 0, title: '' });
    setQuestionForm({ id: 0, quizId: 0, questionText: '' });
    setOptionForm({ id: 0, questionId: 0, optionText: '', optionOrder: 1, correct: false });
  };

  const createUser = async () => {
    await client.post('/admin/users', userForm);
    resetForms();
    load();
  };

  const updateUser = async () => {
    if (!userForm.id) return;
    await client.put(`/admin/users/${userForm.id}`, userForm);
    resetForms();
    load();
  };

  const deleteUser = async (id: number) => {
    await client.delete(`/admin/users/${id}`);
    load();
  };

  const createDictionary = async () => {
    await client.post('/admin/dictionary', dictionaryForm);
    resetForms();
    load();
  };

  const updateDictionary = async () => {
    if (!dictionaryForm.id) return;
    await client.put(`/admin/dictionary/${dictionaryForm.id}`, dictionaryForm);
    resetForms();
    load();
  };

  const deleteDictionary = async (id: number) => {
    await client.delete(`/admin/dictionary/${id}`);
    load();
  };

  const createQuiz = async () => {
    await client.post('/admin/quizzes', { title: quizForm.title });
    resetForms();
    load();
  };

  const updateQuiz = async () => {
    if (!quizForm.id) return;
    await client.put(`/admin/quizzes/${quizForm.id}`, { title: quizForm.title });
    resetForms();
    load();
  };

  const deleteQuiz = async (id: number) => {
    await client.delete(`/admin/quizzes/${id}`);
    load();
  };

  const createQuestion = async () => {
    await client.post('/admin/questions', questionForm);
    resetForms();
    load();
  };

  const updateQuestion = async () => {
    if (!questionForm.id) return;
    await client.put(`/admin/questions/${questionForm.id}`, questionForm);
    resetForms();
    load();
  };

  const deleteQuestion = async (id: number) => {
    await client.delete(`/admin/questions/${id}`);
    load();
  };

  const createOption = async () => {
    await client.post('/admin/options', optionForm);
    resetForms();
    load();
  };

  const updateOption = async () => {
    if (!optionForm.id) return;
    await client.put(`/admin/options/${optionForm.id}`, optionForm);
    resetForms();
    load();
  };

  const deleteOption = async (id: number) => {
    await client.delete(`/admin/options/${id}`);
    load();
  };

  return (
    <div className="admin-layout">
      <aside className="admin-sidebar">
        <h2 className="admin-title">Django Admin</h2>
        <div className="admin-section">Database</div>
        {tabs.map((tab) => (
          <button
            key={tab.key}
            className={`admin-nav ${activeTab === tab.key ? 'active' : ''}`}
            onClick={() => {
              setActiveTab(tab.key);
              resetForms();
            }}
          >
            {tab.label}
          </button>
        ))}
      </aside>

      <main className="admin-main">
        <div className="admin-header">
          <h1>{headerTitle}</h1>
          <div className="admin-actions">
            <button onClick={load}>Refresh</button>
            <button onClick={resetForms}>Clear</button>
          </div>
        </div>

        {loading && <p>로딩 중...</p>}
        {error && <p className="error">{error}</p>}

        {activeTab === 'users' && (
          <div className="admin-grid">
            <section className="card admin-card">
              <h3>User Form</h3>
              <div className="grid">
                <input placeholder="User ID" value={userForm.userId} onChange={(e) => setUserForm({ ...userForm, userId: e.target.value })} />
                <input placeholder="Username" value={userForm.username} onChange={(e) => setUserForm({ ...userForm, username: e.target.value })} />
                <input placeholder="Password (optional)" value={userForm.password} onChange={(e) => setUserForm({ ...userForm, password: e.target.value })} />
                <input placeholder="Email" value={userForm.email} onChange={(e) => setUserForm({ ...userForm, email: e.target.value })} />
                <select value={userForm.role} onChange={(e) => setUserForm({ ...userForm, role: e.target.value as AdminUser['role'] })}>
                  <option value="GENERAL">GENERAL</option>
                  <option value="ADMIN">ADMIN</option>
                </select>
                <select value={userForm.status} onChange={(e) => setUserForm({ ...userForm, status: e.target.value as AdminUser['status'] })}>
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="DEACTIVATED">DEACTIVATED</option>
                </select>
                <div className="admin-form-actions">
                  <button onClick={createUser}>Create</button>
                  <button onClick={updateUser}>Update</button>
                </div>
              </div>
            </section>

            <section className="card admin-card">
              <h3>User List</h3>
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>User ID</th>
                    <th>Username</th>
                    <th>Role</th>
                    <th>Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((user) => (
                    <tr key={user.id} onClick={() => setUserForm({ ...userForm, id: user.id, userId: user.userId, username: user.username, email: user.email ?? '', password: '', role: user.role, status: user.status })}>
                      <td>{user.id}</td>
                      <td>{user.userId}</td>
                      <td>{user.username}</td>
                      <td>{user.role}</td>
                      <td>{user.status}</td>
                      <td>
                        <button className="danger" onClick={(e) => { e.stopPropagation(); deleteUser(user.id); }}>Delete</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          </div>
        )}

        {activeTab === 'dictionary' && (
          <div className="admin-grid">
            <section className="card admin-card">
              <h3>Dictionary Form</h3>
              <div className="grid">
                <input placeholder="Word" value={dictionaryForm.word} onChange={(e) => setDictionaryForm({ ...dictionaryForm, word: e.target.value })} />
                <input placeholder="Meaning" value={dictionaryForm.meaning} onChange={(e) => setDictionaryForm({ ...dictionaryForm, meaning: e.target.value })} />
                <input placeholder="English Word" value={dictionaryForm.englishWord} onChange={(e) => setDictionaryForm({ ...dictionaryForm, englishWord: e.target.value })} />
                <input placeholder="English Meaning" value={dictionaryForm.englishMeaning} onChange={(e) => setDictionaryForm({ ...dictionaryForm, englishMeaning: e.target.value })} />
                <div className="admin-form-actions">
                  <button onClick={createDictionary}>Create</button>
                  <button onClick={updateDictionary}>Update</button>
                </div>
              </div>
            </section>

            <section className="card admin-card">
              <h3>Dictionary List</h3>
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Word</th>
                    <th>Meaning</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {dictionary.map((entry) => (
                    <tr key={entry.id} onClick={() => setDictionaryForm({ ...dictionaryForm, id: entry.id, word: entry.word, meaning: entry.meaning, englishWord: entry.englishWord ?? '', englishMeaning: entry.englishMeaning ?? '' })}>
                      <td>{entry.id}</td>
                      <td>{entry.word}</td>
                      <td>{entry.meaning}</td>
                      <td>
                        <button className="danger" onClick={(e) => { e.stopPropagation(); deleteDictionary(entry.id); }}>Delete</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          </div>
        )}

        {activeTab === 'quizzes' && (
          <div className="admin-grid">
            <section className="card admin-card">
              <h3>Quiz Form</h3>
              <div className="grid">
                <input placeholder="Title" value={quizForm.title} onChange={(e) => setQuizForm({ ...quizForm, title: e.target.value })} />
                <div className="admin-form-actions">
                  <button onClick={createQuiz}>Create</button>
                  <button onClick={updateQuiz}>Update</button>
                </div>
              </div>
            </section>

            <section className="card admin-card">
              <h3>Quiz List</h3>
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Quiz UID</th>
                    <th>Title</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {quizzes.map((quiz) => (
                    <tr key={quiz.id} onClick={() => setQuizForm({ id: quiz.id, title: quiz.title })}>
                      <td>{quiz.id}</td>
                      <td>{quiz.quizId}</td>
                      <td>{quiz.title}</td>
                      <td>
                        <button className="danger" onClick={(e) => { e.stopPropagation(); deleteQuiz(quiz.id); }}>Delete</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          </div>
        )}

        {activeTab === 'questions' && (
          <div className="admin-grid">
            <section className="card admin-card">
              <h3>Question Form</h3>
              <div className="grid">
                <input placeholder="Quiz ID (numeric)" value={questionForm.quizId || ''} onChange={(e) => setQuestionForm({ ...questionForm, quizId: Number(e.target.value) })} />
                <input placeholder="Question Text" value={questionForm.questionText} onChange={(e) => setQuestionForm({ ...questionForm, questionText: e.target.value })} />
                <div className="admin-form-actions">
                  <button onClick={createQuestion}>Create</button>
                  <button onClick={updateQuestion}>Update</button>
                </div>
              </div>
            </section>

            <section className="card admin-card">
              <h3>Question List</h3>
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Quiz ID</th>
                    <th>Text</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {questions.map((q) => (
                    <tr key={q.id} onClick={() => setQuestionForm({ id: q.id, quizId: q.quizId, questionText: q.questionText })}>
                      <td>{q.id}</td>
                      <td>{q.quizId}</td>
                      <td>{q.questionText}</td>
                      <td>
                        <button className="danger" onClick={(e) => { e.stopPropagation(); deleteQuestion(q.id); }}>Delete</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          </div>
        )}

        {activeTab === 'options' && (
          <div className="admin-grid">
            <section className="card admin-card">
              <h3>Option Form</h3>
              <div className="grid">
                <input placeholder="Question ID (numeric)" value={optionForm.questionId || ''} onChange={(e) => setOptionForm({ ...optionForm, questionId: Number(e.target.value) })} />
                <input placeholder="Option Text" value={optionForm.optionText} onChange={(e) => setOptionForm({ ...optionForm, optionText: e.target.value })} />
                <input placeholder="Order" value={optionForm.optionOrder} onChange={(e) => setOptionForm({ ...optionForm, optionOrder: Number(e.target.value) })} />
                <label className="checkbox">
                  <input type="checkbox" checked={optionForm.correct} onChange={(e) => setOptionForm({ ...optionForm, correct: e.target.checked })} />
                  Correct
                </label>
                <div className="admin-form-actions">
                  <button onClick={createOption}>Create</button>
                  <button onClick={updateOption}>Update</button>
                </div>
              </div>
            </section>

            <section className="card admin-card">
              <h3>Option List</h3>
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Question ID</th>
                    <th>Text</th>
                    <th>Order</th>
                    <th>Correct</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {options.map((opt) => (
                    <tr key={opt.id} onClick={() => setOptionForm({ id: opt.id, questionId: opt.questionId, optionText: opt.optionText, optionOrder: opt.optionOrder, correct: opt.correct })}>
                      <td>{opt.id}</td>
                      <td>{opt.questionId}</td>
                      <td>{opt.optionText}</td>
                      <td>{opt.optionOrder}</td>
                      <td>{opt.correct ? 'Y' : 'N'}</td>
                      <td>
                        <button className="danger" onClick={(e) => { e.stopPropagation(); deleteOption(opt.id); }}>Delete</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          </div>
        )}
      </main>
    </div>
  );
}
