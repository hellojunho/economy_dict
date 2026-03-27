import fs from 'node:fs/promises';
import path from 'node:path';

function formatDateKey(date = new Date()) {
  const yy = String(date.getFullYear()).slice(-2);
  const mm = String(date.getMonth() + 1).padStart(2, '0');
  const dd = String(date.getDate()).padStart(2, '0');
  return `${yy}-${mm}-${dd}`;
}

async function main() {
  const [agent, role, content, dateKeyArg] = process.argv.slice(2);

  if (!agent || !role || !content) {
    console.error('Usage: node scripts/append-agent-chat-log.mjs <codex|claude> <role> <content> [YY-MM-DD]');
    process.exit(1);
  }

  const normalizedAgent = agent.trim().toLowerCase();
  if (!['codex', 'claude'].includes(normalizedAgent)) {
    console.error('agent must be one of: codex, claude');
    process.exit(1);
  }

  const dirName = normalizedAgent === 'codex' ? '.codex' : '.claude';
  const dateKey = dateKeyArg?.trim() || formatDateKey();
  const outputDir = path.resolve(process.cwd(), dirName);
  const outputPath = path.join(outputDir, `${normalizedAgent}_${dateKey}.jsonl`);
  const line = JSON.stringify({
    timestamp: new Date().toISOString(),
    agent: normalizedAgent,
    role: role.trim(),
    content
  });

  await fs.mkdir(outputDir, { recursive: true });
  await fs.appendFile(outputPath, `${line}\n`, 'utf8');
  console.log(outputPath);
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(1);
});
