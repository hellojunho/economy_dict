# Project Codex Skills

이 디렉터리는 `/Users/chojunho/project/economy_dict` 전용 Codex skill 모음입니다.

## 목적
- 이 저장소의 아키텍처 규칙을 매번 다시 설명하지 않기
- Spring Boot, React/Vite, OpenAI, Spring Batch, 관리자 업로드 플로우를 일관되게 수정하기
- UI, 상태관리, 프롬프트 설계를 프로젝트 기준으로 고정하기

## 구조
- `/Users/chojunho/project/economy_dict/.codex/skills/economy-dict-fullstack`
- `/Users/chojunho/project/economy_dict/.codex/skills/economy-dict-ui-minimal`
- `/Users/chojunho/project/economy_dict/.codex/skills/economy-dict-pdf-import`
- `/Users/chojunho/project/economy_dict/.codex/skills/economy-dict-glossary-prompting`

각 skill 폴더는 다음 구조를 따릅니다.
- `SKILL.md`: Codex가 실제로 따라야 하는 지침
- `agents/openai.yaml`: 스킬 이름, 짧은 설명, 기본 프롬프트

## 사용하는 방법
이 프로젝트 내부 `.codex` 폴더는 "프로젝트 로컬 스킬 저장소"로 쓰는 방식입니다.

### 1. 직접 경로를 지정해서 사용
가장 확실한 방법입니다.

예시:
```text
Use $economy-dict-fullstack at /Users/chojunho/project/economy_dict/.codex/skills/economy-dict-fullstack to add a new admin analytics endpoint.
```

```text
Use $economy-dict-ui-minimal at /Users/chojunho/project/economy_dict/.codex/skills/economy-dict-ui-minimal to redesign the search page.
```

```text
Use $economy-dict-pdf-import at /Users/chojunho/project/economy_dict/.codex/skills/economy-dict-pdf-import to improve the PDF batch import progress flow.
```

```text
Use $economy-dict-glossary-prompting at /Users/chojunho/project/economy_dict/.codex/skills/economy-dict-glossary-prompting to revise the glossary extraction prompt.
```

### 2. 글로벌 스킬 폴더에 복사 또는 심볼릭 링크
Codex가 자동 발견하는 환경을 쓰고 싶다면, `.codex/skills/*` 를 `$CODEX_HOME/skills` 또는 `~/.codex/skills` 아래로 복사하거나 링크하면 됩니다.

예시:
```bash
mkdir -p ~/.codex/skills
ln -s /Users/chojunho/project/economy_dict/.codex/skills/economy-dict-fullstack ~/.codex/skills/economy-dict-fullstack
```

이 방식은 여러 세션에서 같은 스킬을 재사용할 때 유리합니다.

## 언제 어떤 스킬을 쓰는지
### `economy-dict-fullstack`
사용 시점:
- API, DTO, entity, React page, Docker 설정이 함께 바뀌는 작업
- 인증/인가/JWT/관리자 플로우를 수정하는 작업
- 이 저장소의 전체 구조를 이해하고 기능을 추가하는 작업

### `economy-dict-ui-minimal`
사용 시점:
- 흰색 기반 미니멀 UI 유지가 필요한 작업
- auth, search, admin, chat 화면 레이아웃을 정리하는 작업
- 버튼/폼/테이블 톤을 프로젝트 기준으로 맞추는 작업

### `economy-dict-pdf-import`
사용 시점:
- 관리자 PDF 업로드, Spring Batch, Task 상태 관리 수정
- 업로드 제한, 비동기 진행률, 에러 로그, 작업 이력 수정
- OpenAI를 통한 용어 추출 import 플로우 수정

### `economy-dict-glossary-prompting`
사용 시점:
- 경제 용어 추출 프롬프트 수정
- JSON 스키마 강제, 중복 제거, DB 적재 형식 조정
- OpenAI 응답 품질을 높이기 위한 prompt engineering 작업

## 작업 절차 권장안
1. 먼저 적절한 스킬을 하나 고릅니다.
2. 요청이 복합적이면, 가장 상위 범위를 가진 스킬부터 씁니다.
3. UI와 백엔드가 같이 바뀌면 `economy-dict-fullstack`를 우선 사용합니다.
4. 프롬프트만 손보면 `economy-dict-glossary-prompting`만 씁니다.
5. 업로드/배치만 손보면 `economy-dict-pdf-import`만 씁니다.

## 권장 프롬프트 예시
### 새 기능 추가
```text
Use $economy-dict-fullstack at /Users/chojunho/project/economy_dict/.codex/skills/economy-dict-fullstack and add a new analytics tab to the admin page.
```

### UI 리디자인
```text
Use $economy-dict-ui-minimal at /Users/chojunho/project/economy_dict/.codex/skills/economy-dict-ui-minimal and simplify the sign-in and sign-up pages.
```

### PDF 배치 개선
```text
Use $economy-dict-pdf-import at /Users/chojunho/project/economy_dict/.codex/skills/economy-dict-pdf-import and make task progress updates more granular.
```

### OpenAI 프롬프트 개선
```text
Use $economy-dict-glossary-prompting at /Users/chojunho/project/economy_dict/.codex/skills/economy-dict-glossary-prompting and make the extractor stricter about null handling.
```

## 유지보수 원칙
- 스킬 내용은 프로젝트 코드가 바뀔 때 같이 업데이트합니다.
- 스킬은 장황하게 쓰지 말고, 이 저장소에서만 필요한 규칙만 유지합니다.
- 중복 설명은 줄이고, 실제 코드 경로와 엔드포인트 규칙을 우선 기록합니다.
- 프론트/백엔드 구조가 바뀌면 `SKILL.md`의 경로와 규칙을 먼저 수정합니다.

## 현재 상태관리 규칙
프론트 상태관리는 `zustand` 기준입니다.

주요 스토어:
- `/Users/chojunho/project/economy_dict/frontend/src/stores/authStore.ts`
- `/Users/chojunho/project/economy_dict/frontend/src/stores/chatStore.ts`
- `/Users/chojunho/project/economy_dict/frontend/src/stores/searchStore.ts`

새 전역 상태를 추가할 때는 기존 스토어를 확장하거나, 목적별 새 스토어를 `frontend/src/stores` 아래에 추가합니다.
