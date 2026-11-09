# docs

개발 문서의 목차와 작업 유형별로 읽을 문서다.

## 문서 목록

| 문서 | 역할 |
| --- | --- |
| [architecture.md](architecture.md) | 시스템 구성, 패키지 구조, 의존 방향 |
| [layer-boundaries.md](layer-boundaries.md) | Controller, Service, Repository, Entity, DTO의 책임과 네이밍 |
| [api-conventions.md](api-conventions.md) | API URL, 인증, 오류 응답, ErrorCode 규칙의 정본 |
| [persistence.md](persistence.md) | 엔티티, 시간 타입, 트랜잭션, 마이그레이션 규칙 |
| [testing.md](testing.md) | 테스트 종류와 네이밍, 로컬 DB |
| [development-cycle.md](development-cycle.md) | 브랜치, 커밋, PR, 병합 절차의 정본 |
| [quality-gates.md](quality-gates.md) | PR 전 확인 기준 |
| [domains/](domains/) | 도메인별 현재 규칙과 결정 기록: [restaurant](domains/restaurant.md) · [recommendation](domains/recommendation.md) · [pick](domains/pick.md) · [review](domains/review.md) · [discovery](domains/discovery.md) · [member](domains/member.md) · [admin](domains/admin.md) |
| [progress.md](progress.md) | 다음 작업 순서, 구현 현황 체크리스트, 운영 장애 기록 |
| [DEPLOYMENT.md](DEPLOYMENT.md) | EC2 배포, 환경변수, 롤백 |
| [OPERATIONS.md](OPERATIONS.md) | 운영 중 수동 작업(식당 신고 검토 등) |
| [workflows/](workflows/README.md) | 반복하는 개발 절차 |

## 작업 유형별 탐색 경로

| 작업 | 읽을 문서 |
| --- | --- |
| 새 기능 구현 | 해당 도메인 폴더의 AGENTS.md → domains/{도메인}.md → architecture → layer-boundaries → api-conventions → persistence → testing |
| Entity·스키마 변경 | persistence → testing |
| API 추가·변경 | api-conventions → layer-boundaries, 그리고 Notion API SPECIFICATION 갱신 |
| 커밋·PR | development-cycle → quality-gates |
| 배포·운영 설정 | DEPLOYMENT → OPERATIONS |

## 관리 원칙

- 공통 규칙은 해당 주제 문서 한 곳에서만 관리하고, 다른 문서에서는 링크한다
- 결정되지 않은 내용은 미리 쓰지 않는다. 확정된 내용부터 반영한다
- 규칙 문서에는 잘 바뀌지 않는 규칙만 둔다. 추천 정책 같은 기획 내용은 Notion과 `domains/`에 둔다. 도메인 폴더의 `AGENTS.md`에는 그 코드를 고칠 때 꼭 지킬 규칙만 짧게 두고 자세한 내용은 `domains/`로 링크한다
