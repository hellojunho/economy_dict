# Economy Dict

Spring Boot + React + PostgreSQL 기반의 경제 용어 학습 플랫폼입니다.
`docker compose`로 DB, Backend, Frontend(Nginx)를 함께 실행할 수 있습니다.

## 구성

- `db`: PostgreSQL 16
- `backend`: Spring Boot 3.x (`8081`)
- `nginx`: React build 정적 서빙 + `/api` 프록시 (`4321`)

기본 포트:

- Frontend: `4321`
- Backend: `8081`
- Database: `9001`

## 사전 준비

로컬에 아래가 설치되어 있어야 합니다.

- Docker
- Docker Compose

확인:

```bash
docker --version
docker compose version
```

## 환경 확인

백엔드 설정 파일:

- `backend/src/main/resources/application.properties`

중요 항목:

```properties
app.jwt.secret=CHANGE_ME_JWT_SECRET_32_BYTES_MINIMUM_1234567890
openai.api.key=YOUR_OPENAI_API_KEY
server.port=8081
```

주의:

- `app.jwt.secret`는 최소 32바이트 이상이어야 합니다.
- `openai.api.key`가 없으면 AI 검색/업로드 기능은 정상 동작하지 않습니다.

## 전체 서비스 실행

프로젝트 루트에서 실행:

```bash
docker compose up --build
```

백그라운드 실행:

```bash
docker compose up -d --build
```

또는 Makefile 사용:

```bash
make start
```

실행 후 접속:

- Frontend: [http://localhost:4321](http://localhost:4321)
- Backend Health: [http://localhost:8081/api/health](http://localhost:8081/api/health)

## 서비스 중지

```bash
docker compose down
```

또는:

```bash
make stop
```

볼륨까지 함께 삭제:

```bash
docker compose down -v
```

주의:

- `-v`를 사용하면 PostgreSQL 데이터 볼륨도 삭제됩니다.

## 로그 확인

전체 로그:

```bash
docker compose logs -f
```

백엔드 로그만:

```bash
docker compose logs -f backend
```

프론트(Nginx) 로그만:

```bash
docker compose logs -f nginx
```

DB 로그만:

```bash
docker compose logs -f db
```

## 개별 재빌드

백엔드만 다시 빌드:

```bash
docker compose build backend
docker compose up -d backend
```

프론트(Nginx)만 다시 빌드:

```bash
docker compose build nginx
docker compose up -d nginx
```

## 초기 접속 경로

- 메인: `/`
- 로그인: `/signin`
- 회원가입: `/signup`
- 용어 검색: `/words`
- 퀴즈: `/quiz`
- 마이페이지: `/mypage`
- 관리자: `/admin`

## 관리자 계정 생성

```bash
make create-admin
```

생성 계정:

- `id`: `admin`
- `username`: `admin`
- `email`: `admin@admin.com`
- `password`: `admin123!@#`
- `role`: `ADMIN`

## 업로드 관련

PDF 업로드 제한:

- 최대 `20MB`

관련 설정 위치:

- `docker/nginx/nginx.conf`
- `backend/src/main/resources/application.properties`

## Frontend 상태관리

프론트 상태관리는 `zustand` 기준입니다.

현재 주요 스토어:

- `frontend/src/stores/authStore.ts`
- `frontend/src/stores/chatStore.ts`
- `frontend/src/stores/searchStore.ts`
- `frontend/src/stores/adminStore.ts`
- `frontend/src/stores/quizStore.ts`

규칙:

- 인증, 검색, 채팅, 관리자, 퀴즈처럼 여러 컴포넌트에서 재사용되는 상태는 store로 둡니다.
- 일회성 입력 UI가 아닌 이상, 페이지 전용 대형 상태도 store로 분리합니다.
- API 호출 로직은 가능한 store action 안에 둬서 page는 렌더링과 이벤트 연결에 집중시킵니다.

## Codex Skills

프로젝트 전용 Codex skills는 아래 경로에 있습니다.

- `/Users/chojunho/project/economy_dict/.codex/skills`

상세 사용법 문서:

- `/Users/chojunho/project/economy_dict/.codex/README.md`

현재 제공 스킬:

- `economy-dict-fullstack`
- `economy-dict-ui-minimal`
- `economy-dict-pdf-import`
- `economy-dict-glossary-prompting`

### 직접 경로로 사용하는 방법

예시:

```text
Use $economy-dict-fullstack at /Users/chojunho/project/economy_dict/.codex/skills/economy-dict-fullstack to add a new admin analytics endpoint.
```

```text
Use $economy-dict-ui-minimal at /Users/chojunho/project/economy_dict/.codex/skills/economy-dict-ui-minimal to redesign the search page.
```

### 자주 쓰는 스킬을 `~/.codex/skills`로 연결하는 방법

심볼릭 링크 방식:

```bash
mkdir -p ~/.codex/skills
ln -s /Users/chojunho/project/economy_dict/.codex/skills/economy-dict-fullstack ~/.codex/skills/economy-dict-fullstack
ln -s /Users/chojunho/project/economy_dict/.codex/skills/economy-dict-ui-minimal ~/.codex/skills/economy-dict-ui-minimal
ln -s /Users/chojunho/project/economy_dict/.codex/skills/economy-dict-pdf-import ~/.codex/skills/economy-dict-pdf-import
ln -s /Users/chojunho/project/economy_dict/.codex/skills/economy-dict-glossary-prompting ~/.codex/skills/economy-dict-glossary-prompting
```

이미 링크가 있으면 삭제 후 다시 연결:

```bash
rm -f ~/.codex/skills/economy-dict-fullstack
rm -f ~/.codex/skills/economy-dict-ui-minimal
rm -f ~/.codex/skills/economy-dict-pdf-import
rm -f ~/.codex/skills/economy-dict-glossary-prompting
```

그 다음 다시 `ln -s`를 실행하면 됩니다.

## 트러블슈팅

### 1. `403 Forbidden`

다시 빌드 후 실행:

```bash
docker compose down
docker compose up --build
```

### 2. `413 Request Entity Too Large`

현재 업로드 제한은 `20MB`입니다.
더 크게 늘리려면 아래 두 파일을 같이 수정해야 합니다.

- `docker/nginx/nginx.conf`
- `backend/src/main/resources/application.properties`

### 3. Backend가 안 뜨는 경우

헬스 체크:

```bash
curl -i http://localhost:8081/api/health
```

### 4. DB 접속 확인

```bash
docker compose exec db psql -U postgres -d economy_dict
```

## 개발 메모

- Frontend는 Nginx가 정적 파일을 서빙합니다.
- `/api/*` 요청은 Nginx가 Backend로 프록시합니다.
- PostgreSQL 컨테이너는 `db_data` 볼륨을 사용합니다.
