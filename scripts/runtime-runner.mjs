import { spawn } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const rootDir = path.resolve(__dirname, '..');
const runtimeFile = path.join(rootDir, 'runtime.env');
const preferredJavaHome = 'C:\\Program Files\\JetBrains\\PyCharm 2023.3.3\\jbr';

function parseEnvFile(filePath) {
  const file = fs.readFileSync(filePath, 'utf8');
  const values = {};

  for (const rawLine of file.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith('#')) {
      continue;
    }

    const separatorIndex = line.indexOf('=');
    if (separatorIndex < 1) {
      continue;
    }

    const key = line.slice(0, separatorIndex).trim();
    let value = line.slice(separatorIndex + 1).trim();

    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }

    values[key] = value;
  }

  return values;
}

function normalizeRuntime(platform) {
  if (platform === 'win32') {
    return 'windows';
  }

  if (platform === 'darwin') {
    return 'macos';
  }

  return platform;
}

function toAbsolutePath(targetPath) {
  if (path.isAbsolute(targetPath)) {
    return targetPath;
  }

  return path.resolve(rootDir, targetPath);
}

function ensureDirectory(targetPath) {
  fs.mkdirSync(toAbsolutePath(targetPath), { recursive: true });
}

function resolveJavaHome(runtimeEnv) {
  const configured = runtimeEnv.JAVA_HOME?.trim();
  if (configured) {
    return configured;
  }

  if (fs.existsSync(preferredJavaHome)) {
    return preferredJavaHome;
  }

  return process.env.JAVA_HOME;
}

function createLocalEnv(runtimeEnv) {
  const javaHome = resolveJavaHome(runtimeEnv);
  const javaBin = javaHome ? path.join(javaHome, 'bin') : null;
  const mergedPath = javaBin ? `${javaBin}${path.delimiter}${process.env.Path ?? process.env.PATH ?? ''}` : (process.env.Path ?? process.env.PATH ?? '');
  return {
    ...process.env,
    ...runtimeEnv,
    JAVA_HOME: javaHome,
    BACKEND_PORT: runtimeEnv.BACKEND_PORT ?? '8081',
    FRONTEND_PORT: runtimeEnv.FRONTEND_PORT ?? '4321',
    DB_PORT: runtimeEnv.DB_PORT ?? '9001',
    Path: mergedPath,
    PATH: mergedPath,
    GRADLE_USER_HOME: path.join(rootDir, '.gradle-user-home'),
    SPRING_DATASOURCE_URL:
      runtimeEnv.LOCAL_SPRING_DATASOURCE_URL ?? 'jdbc:postgresql://localhost:9001/economy_dict',
    APP_CHAT_STORAGE_PATH: runtimeEnv.LOCAL_CHAT_STORAGE_PATH ?? 'backend/chats',
    APP_ERROR_LOG_STORAGE_PATH: runtimeEnv.LOCAL_LOG_PATH ?? 'backend/logs',
    LOG_PATH: runtimeEnv.LOCAL_LOG_PATH ?? 'backend/logs'
  };
}

function spawnCommand(command, args, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      cwd: options.cwd ?? rootDir,
      env: options.env ?? process.env,
      stdio: options.stdio ?? 'inherit',
      shell: false
    });

    child.on('error', reject);
    child.on('close', (code) => {
      if (code === 0) {
        resolve();
        return;
      }

      reject(new Error(`${command} ${args.join(' ')} failed with exit code ${code ?? 'unknown'}`));
    });
  });
}

function captureCommand(command, args, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      cwd: options.cwd ?? rootDir,
      env: options.env ?? process.env,
      stdio: ['ignore', 'pipe', 'pipe'],
      shell: false
    });

    let stdout = '';
    let stderr = '';

    child.stdout.on('data', (chunk) => {
      stdout += chunk.toString();
    });

    child.stderr.on('data', (chunk) => {
      stderr += chunk.toString();
    });

    child.on('error', reject);
    child.on('close', (code) => {
      if (code === 0) {
        resolve({ stdout, stderr });
        return;
      }

      reject(
        new Error(
          `${command} ${args.join(' ')} failed with exit code ${code ?? 'unknown'}${stderr ? `: ${stderr.trim()}` : ''}`
        )
      );
    });
  });
}

async function waitForCommand(command, args, options = {}) {
  const attempts = options.attempts ?? 30;
  const delayMs = options.delayMs ?? 2000;
  const validate = options.validate ?? (() => true);

  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    try {
      const result = await captureCommand(command, args, options);
      if (validate(result)) {
        return;
      }
      throw new Error('Validation failed');
    } catch (error) {
      if (attempt === attempts) {
        throw error;
      }

      await new Promise((resolve) => setTimeout(resolve, delayMs));
    }
  }
}

async function main() {
  if (!fs.existsSync(runtimeFile)) {
    throw new Error(`Missing runtime file: ${runtimeFile}`);
  }

  const runtimeEnv = parseEnvFile(runtimeFile);
  const selectedRuntime = (runtimeEnv.APP_RUNTIME ?? '').trim().toLowerCase();
  const actualRuntime = normalizeRuntime(process.platform);

  if (!['windows', 'macos'].includes(selectedRuntime)) {
    throw new Error('APP_RUNTIME must be either "windows" or "macos" in runtime.env.');
  }

  if (actualRuntime !== selectedRuntime) {
    throw new Error(`runtime.env sets APP_RUNTIME=${selectedRuntime}, but this machine is ${actualRuntime}.`);
  }

  ensureDirectory(runtimeEnv.LOCAL_LOG_PATH ?? 'backend/logs');
  ensureDirectory(runtimeEnv.LOCAL_CHAT_STORAGE_PATH ?? 'backend/chats');

  const localEnv = createLocalEnv(runtimeEnv);
  const backendDir = path.join(rootDir, 'backend');
  const frontendDir = path.join(rootDir, 'frontend');
  const dockerArgsBase = ['compose', '--env-file', runtimeFile];
  const command = process.argv[2];

  switch (command) {
    case 'docker-up':
      await spawnCommand('docker', [...dockerArgsBase, 'up', '--build']);
      return;
    case 'docker-down':
      await spawnCommand('docker', [...dockerArgsBase, 'down']);
      return;
    case 'docker-logs':
      await spawnCommand('docker', [...dockerArgsBase, 'logs', '-f']);
      return;
    case 'backend': {
      const gradleCommand =
        selectedRuntime === 'windows'
          ? path.join(backendDir, 'gradlew.bat')
          : path.join(backendDir, 'gradlew');
      await spawnCommand(gradleCommand, ['bootRun'], { cwd: backendDir, env: localEnv });
      return;
    }
    case 'backend-test': {
      const gradleCommand =
        selectedRuntime === 'windows'
          ? path.join(backendDir, 'gradlew.bat')
          : path.join(backendDir, 'gradlew');
      await spawnCommand(gradleCommand, ['test'], { cwd: backendDir, env: localEnv });
      return;
    }
    case 'frontend': {
      await spawnCommand(
        'node',
        [path.join(frontendDir, 'node_modules', 'vite', 'bin', 'vite.js'), '--port', localEnv.FRONTEND_PORT ?? '4321', '--host'],
        { cwd: frontendDir, env: localEnv }
      );
      return;
    }
    case 'frontend-build': {
      await spawnCommand(
        'node',
        [path.join(frontendDir, 'node_modules', 'typescript', 'bin', 'tsc'), '-b'],
        { cwd: frontendDir, env: localEnv }
      );
      await spawnCommand(
        'node',
        [path.join(frontendDir, 'node_modules', 'vite', 'bin', 'vite.js'), 'build'],
        { cwd: frontendDir, env: localEnv }
      );
      return;
    }
    case 'create-admin':
      await spawnCommand('docker', [...dockerArgsBase, 'up', '-d', 'db', 'backend']);
      await waitForCommand(
        'docker',
        [...dockerArgsBase, 'exec', '-T', 'db', 'pg_isready', '-U', runtimeEnv.POSTGRES_USER ?? 'postgres', '-d', runtimeEnv.POSTGRES_DB ?? 'economy_dict'],
        { env: process.env }
      );
      await waitForCommand(
        'docker',
        [
          ...dockerArgsBase,
          'exec',
          '-T',
          'db',
          'psql',
          '-U',
          runtimeEnv.POSTGRES_USER ?? 'postgres',
          '-d',
          runtimeEnv.POSTGRES_DB ?? 'economy_dict',
          '-tAc',
          "SELECT 1 FROM information_schema.tables WHERE table_name='users'"
        ],
        {
          env: process.env,
          validate: ({ stdout }) => stdout.trim() === '1'
        }
      );
      await spawnCommand('docker', [
        ...dockerArgsBase,
        'exec',
        '-T',
        'db',
        'psql',
        '-U',
        runtimeEnv.POSTGRES_USER ?? 'postgres',
        '-d',
        runtimeEnv.POSTGRES_DB ?? 'economy_dict',
        '-c',
        'CREATE EXTENSION IF NOT EXISTS pgcrypto;'
      ]);
      await spawnCommand('docker', [
        ...dockerArgsBase,
        'exec',
        '-T',
        'db',
        'psql',
        '-U',
        runtimeEnv.POSTGRES_USER ?? 'postgres',
        '-d',
        runtimeEnv.POSTGRES_DB ?? 'economy_dict',
        '-c',
        "INSERT INTO users (user_id, username, password, user_email, role, status, activated_at, deactivated_at, created_at, updated_at) VALUES ('admin', 'admin', crypt('admin123!@#', gen_salt('bf')), 'admin@admin.com', 'ADMIN', 'ACTIVE', NOW(), NULL, NOW(), NOW()) ON CONFLICT (user_id) DO UPDATE SET username = EXCLUDED.username, password = crypt('admin123!@#', gen_salt('bf')), user_email = EXCLUDED.user_email, role = 'ADMIN', status = 'ACTIVE', activated_at = COALESCE(users.activated_at, NOW()), deactivated_at = NULL, updated_at = NOW();"
      ]);
      return;
    default:
      throw new Error(
        'Usage: node scripts/runtime-runner.mjs <docker-up|docker-down|docker-logs|backend|backend-test|frontend|frontend-build|create-admin>'
      );
  }
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
