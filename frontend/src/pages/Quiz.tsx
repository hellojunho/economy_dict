import { useEffect, useState } from 'react';
import client from '../api/client';

interface QuizSummary {
  quizId: string;
  title: string;
}

interface QuizOption {
  id: number;
  optionText: string;
  optionOrder: number;
}

interface QuizQuestion {
  id: number;
  questionText: string;
  options: QuizOption[];
}

interface QuizDetail {
  quizId: string;
  title: string;
  questions: QuizQuestion[];
}

export default function Quiz() {
  const [quizzes, setQuizzes] = useState<QuizSummary[]>([]);
  const [selected, setSelected] = useState<QuizDetail | null>(null);
  const [answers, setAnswers] = useState<Record<number, number>>({});
  const [result, setResult] = useState('');

  useEffect(() => {
    client.get('/quizzes').then((res) => setQuizzes(res.data));
  }, []);

  const loadQuiz = async (quizId: string) => {
    const res = await client.get(`/quizzes/${quizId}`);
    setSelected(res.data);
    setAnswers({});
    setResult('');
  };

  const submit = async () => {
    if (!selected) return;
    const payload = {
      answers: Object.entries(answers).map(([questionId, optionId]) => ({
        questionId: Number(questionId),
        selectedOptionId: optionId
      }))
    };
    const res = await client.post(`/quizzes/${selected.quizId}/submit`, payload);
    setResult(`정답 ${res.data.correctCount}/${res.data.totalQuestions} (완료: ${res.data.completed})`);
  };

  return (
    <div className="container grid grid-2">
      <section className="card">
        <h2>퀴즈 목록</h2>
        <div className="grid">
          {quizzes.map((quiz) => (
            <button key={quiz.quizId} onClick={() => loadQuiz(quiz.quizId)}>
              {quiz.title}
            </button>
          ))}
        </div>
      </section>

      {selected && (
        <section className="card">
          <h2>{selected.title}</h2>
          <div className="grid">
            {selected.questions.map((q) => (
              <div key={q.id} className="card">
                <p>{q.questionText}</p>
                <div className="grid">
                  {q.options.map((opt) => (
                    <label key={opt.id}>
                      <input
                        type="radio"
                        name={`q-${q.id}`}
                        checked={answers[q.id] === opt.id}
                        onChange={() => setAnswers({ ...answers, [q.id]: opt.id })}
                      />
                      {opt.optionText}
                    </label>
                  ))}
                </div>
              </div>
            ))}
          </div>
          <button onClick={submit}>제출</button>
          {result && <p>{result}</p>}
        </section>
      )}
    </div>
  );
}
