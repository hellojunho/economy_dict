import { useState } from 'react';
import client from '../api/client';

interface Entry {
  id: number;
  word: string;
  meaning: string;
  englishWord?: string;
  englishMeaning?: string;
}

export default function Dictionary() {
  const [query, setQuery] = useState('');
  const [entries, setEntries] = useState<Entry[]>([]);

  const search = async () => {
    const res = await client.get('/dictionary', { params: { q: query } });
    setEntries(res.data);
  };

  return (
    <div className="container">
      <section className="card">
        <h2>사전 검색</h2>
        <div className="grid">
          <input placeholder="단어" value={query} onChange={(e) => setQuery(e.target.value)} />
          <button onClick={search}>검색</button>
        </div>
      </section>

      <section className="grid">
        {entries.map((entry) => (
          <div key={entry.id} className="card">
            <h3>{entry.word}</h3>
            <p>{entry.meaning}</p>
            {entry.englishWord && <p>EN: {entry.englishWord}</p>}
            {entry.englishMeaning && <p>EN Meaning: {entry.englishMeaning}</p>}
          </div>
        ))}
      </section>
    </div>
  );
}
