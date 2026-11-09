# AGENTS.md

AI 개발 도구(Claude Code, Codex 등)가 작업 전에 확인할 공통 지침과 문서 탐색 순서다.

## 문서 읽기 순서

1. [README.md](README.md)에서 실행 방법과 현재 구현 범위를 확인한다
2. [docs/README.md](docs/README.md)에서 작업별로 읽을 문서를 확인한다
3. 현재 작업과 관련된 문서만 골라 읽는다
4. 현재 결정은 [docs/SERVICE_DECISIONS.md](docs/SERVICE_DECISIONS.md), 결정 배경은 [docs/history.md](docs/history.md)에서 확인한다

## 공통 지침

- 문서에 정해진 규칙을 따른다. 문서에 없는 규칙은 임의로 정하지 않고 먼저 확인한다
- 이 파일과 `docs/`의 규칙 문서에는 잘 바뀌지 않는 규칙만 둔다. API 명세·기획은 Notion이 원본이다
  - API 명세: Notion BE > API SPECIFICATION
  - 코드·네이밍 컨벤션 원본, ERD: Notion [BE](https://app.notion.com/p/3d4482bdb23e80038885edb637243716)
- 브랜치·커밋·PR은 [docs/development-cycle.md](docs/development-cycle.md)를 따른다
- PR 전에는 [docs/quality-gates.md](docs/quality-gates.md)를 확인한다
- 문서, PR 제목·본문, 사용자에게 하는 답변은 한국어로 쓴다. 코드 식별자와 커밋 type 접두사는 영어 그대로 둔다
- 여러 에이전트에게 작업을 나눌 때는 [docs/workflows/orchestration.md](docs/workflows/orchestration.md)를 따른다

## 작업 시작·완료 형식

작업을 시작하기 전에 아래 항목을 적고, [docs/progress.md](docs/progress.md)의 "구현 현황"에 `- [ ]` 항목을 추가한다.

| 항목 | 내용 |
| --- | --- |
| Goal | 작업이 끝났을 때의 결과 |
| Constraints | 지켜야 할 제약 |
| Done | 완료 조건 |

작업을 마치면 구현 현황 항목을 `- [x]`로 체크하고(PR 번호 포함) 다음 작업을 새 항목으로 추가한 뒤, 아래 내용을 남긴다.

- 실행한 명령과 결과
- 실행하지 못한 검사와 이유
- 남은 위험

## 개인 작업 폴더

개인 메모, 작업 계획, 실험 코드는 레포 루트의 `.local/`에 둔다. `.local/`은 커밋하지 않는다.

```plain text
.local/
├── notes/       # 개인 메모
├── plans/       # 작업 계획·진행 기록
└── prototypes/  # 실험 코드
```
