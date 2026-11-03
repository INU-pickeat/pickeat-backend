# Development Cycle

브랜치, 커밋, PR, 병합 절차다.
원본: Notion [Git Branch Naming Convention](https://app.notion.com/p/d9f482bdb23e823da7da019c93d5c3ee), [Git Commit Message Convention](https://app.notion.com/p/b61482bdb23e82a5b8de010191af9500).

## 브랜치

`main` 하나만 쓴다. `develop` 같은 통합 브랜치는 두지 않는다.

- 작업마다 최신 `main`에서 브랜치를 만들고 `main`으로 PR을 보낸다
- `main`에 직접 커밋·푸시하지 않는다
- 하나의 브랜치에는 하나의 작업 목적만 담는다

### 이름 형식

```plain text
<prefix>/<description>
```

| Prefix | 사용 기준 | 예시 |
| --- | --- | --- |
| `feat/` | 새 기능 | `feat/admin-dashboard` |
| `fix/` | 버그 수정 | `fix/default-search-radius` |
| `refactor/` | 동작 변화 없는 구조 개선 | `refactor/member-instant` |
| `docs/` | 문서만 변경 | `docs/dev-conventions` |
| `chore/` | 설정·빌드·배포 등 보조 작업 | `chore/ci-setup` |

- 영문 소문자와 숫자, 단어 구분은 하이픈(-)

## 커밋 메시지

```plain text
<type>(<scope>): <description>
```

```plain text
feat(recommendation): 추천 검색 반경 선택 추가
fix(restaurant): 식당 상세 한줄평 대체 적용
refactor(member): 회원 가입·수정 시각을 Instant로 통일
docs: 관리자 대시보드 로드맵 체크
```

### Type

| Type | 사용 기준 |
| --- | --- |
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변화 없는 구조 개선 |
| `test` | 테스트만 추가·수정 |
| `docs` | 문서 |
| `chore` | 빌드, 의존성, 설정, 배포 스크립트 등 |

### Scope

도메인 패키지 이름을 쓴다. 중심 도메인이 없는 변경은 생략한다.

| Scope | 범위 |
| --- | --- |
| `auth` | 회원가입·로그인, 이메일 인증, JWT·refresh token |
| `member` | 회원 정보·프로필 |
| `restaurant` | 식당 데이터, Google Places 연동, 식당 신고 |
| `recommendation` | 추천 세션·점수·재추천 |
| `pick` | Pick 생성·상태, 최근 Pick, Pick 지도·캘린더 |
| `review` | 후기, 피드, 좋아요, 후기 이미지 |
| `discovery` | 초기 탐색 스팟 |
| `admin` | 관리자 대시보드 |
| `global` | 공통 예외 응답, 보안 공통 설정 |
| `deploy` | EC2 배포, CI/CD |

### Description

- 한글, 결과 중심의 명사형으로 쓴다. 끝에 마침표를 찍지 않는다
- 하나의 커밋에는 하나의 목적만 담는다. 구현과 그 구현의 테스트는 같은 커밋에 넣어도 된다

## PR·병합

- PR 제목은 커밋 메시지 형식을 따른다. squash merge라 `main`에는 PR 제목이 `(#번호)`와 함께 남는다
- PR 제목의 설명과 본문은 한국어로 쓰고 [PR 템플릿](../.github/pull_request_template.md)을 따른다
- API를 추가하거나 계약을 바꾸면 Notion API SPECIFICATION 항목도 고친다. 머지 전에는 개발현황을 "진행 중"으로 두고 참고사항에 PR 번호를 적는다
- 병합된 브랜치는 GitHub 설정(`delete_branch_on_merge`)으로 자동 삭제된다
- 작업을 시작할 때와 끝낼 때 `docs/SERVICE_DECISIONS.md`의 "구현 현황"을 갱신한다 ([AGENTS.md](../AGENTS.md#작업-시작완료-형식))
