# Economy Dict

Windows와 macOS에서 같은 방식으로 실행할 수 있도록 루트 실행 설정 파일과 공용 실행 명령을 정리했습니다.

## 실행 설정

프로젝트 루트의 `runtime.env` 파일에서 현재 실행 환경을 지정합니다.

```env
APP_RUNTIME=windows
```

또는

```env
APP_RUNTIME=macos
```

같은 파일에서 포트, DB 접속값, JWT 시크릿, OpenAI 설정도 함께 관리합니다.

## 사전 준비

- Docker Desktop
- Node.js 20 이상
- 로컬 백엔드 실행 시 JDK 17

## 공통 실행 명령

루트에서 실행합니다.

전체 Docker 실행:

```bash
npm run start
```

production Docker 실행:

```bash
make start-prod
```

Windows에서는 아래처럼 기존 방식도 사용할 수 있습니다.

```powershell
make start
```

중지:

```bash
npm run stop
```

production 중지:

```bash
make stop-prod
```

로그:

```bash
npm run logs
```

## 로컬 개발 실행

백엔드:

```bash
npm run backend
```

프런트:

```bash
npm run frontend
```

관리자 계정 생성:

```bash
npm run create-admin
```

## 검증 명령

프런트 빌드:

```bash
npm run frontend:build
```

백엔드 테스트:

```bash
npm run backend:test
```

## 기본 포트

- Frontend: `5555`
- Backend: `8081`
- PostgreSQL: `9001`

필요하면 `runtime.env`에서 변경하면 됩니다.

## 기본 관리자 계정

- `id`: `admin`
- `username`: `admin`
- `email`: `admin@admin.com`
- `password`: `admin123!@#`

## 주요 설정 키

- `APP_RUNTIME`: `windows` 또는 `macos`
- `FRONTEND_PORT`
- `BACKEND_PORT`
- `DB_PORT`
- `LOCAL_SPRING_DATASOURCE_URL`
- `DOCKER_SPRING_DATASOURCE_URL`
- `APP_JWT_SECRET`
- `OPENAI_API_KEY`
- `OPENAI_API_MODEL`
