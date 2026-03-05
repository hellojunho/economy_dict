import { useState } from 'react';
import client from '../api/client';

export default function Chat() {
  const [message, setMessage] = useState('');
  const [reply, setReply] = useState('');

  const send = async () => {
    const res = await client.post('/chat', { message });
    setReply(res.data.reply);
  };

  return (
    <div className="container">
      <section className="card">
        <h2>ChatGPT</h2>
        <div className="grid">
          <textarea rows={4} value={message} onChange={(e) => setMessage(e.target.value)} />
          <button onClick={send}>보내기</button>
        </div>
      </section>

      {reply && (
        <section className="card">
          <h3>응답</h3>
          <p>{reply}</p>
        </section>
      )}
    </div>
  );
}
