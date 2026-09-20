# Pick Eat Backend

PickEat 프로젝트의 백엔드 서버입니다.

## 기술 스택

- Java 21, Spring Boot 4.1.1
- Spring Data JPA, Flyway (PostgreSQL + PostGIS)
- Spring Security + JWT (자체 로그인)
- springdoc-openapi (Swagger UI)

## 로컬 실행

### 1. PostgreSQL 준비

기본 설정은 아래 환경변수를 사용합니다 (모두 오버라이드 가능, `src/main/resources/application.yaml` 참고).

| 환경변수 | 기본값 |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/pick_eat` |
| `DB_USERNAME` | 현재 OS 사용자명 |
| `DB_PASSWORD` | (빈 값) |
| `JWT_SECRET` | 로컬 개발용 기본값 포함 (운영 배포 시 반드시 변경) |
| `JWT_ACCESS_TOKEN_VALIDITY_MS` | `1800000` (30분) |

로컬에 `pick_eat`라는 이름의 데이터베이스를 미리 만들어두면 됩니다. 스키마는 Flyway 마이그레이션(`src/main/resources/db/migration`)이 기동 시 자동으로 적용합니다.

### 2. 서버 실행

```bash
./gradlew bootRun
```

### 3. API 테스트 (Swagger UI)

서버가 뜨면 `http://localhost:8080/swagger-ui/index.html` 에서 API를 바로 테스트할 수 있습니다.

1. `POST /api/v1/auth/signup`으로 회원가입
2. `POST /api/v1/auth/login`으로 로그인 후 `accessToken` 발급
3. 우측 상단 `Authorize` 버튼에 `Bearer <accessToken>` 입력
4. 인증이 필요한 API 호출

## 테스트

```bash
./gradlew test
```

통합 테스트는 로컬 `pick_eat` 데이터베이스에 Flyway 마이그레이션을 적용합니다. PostgreSQL에
PostGIS가 설치되어 있어야 하며, CI에서는 별도의 PostGIS 서비스 데이터베이스를 사용합니다.

## 현재 구현 범위

**M1 Restaurant 마일스톤 완료 (2026-09-20). 다음은 M2 추천.**

- Member 회원가입·로그인과 JWT 인증 (`POST /api/v1/auth/signup`, `/login`)
- 식당 상세 조회 (`GET /api/v1/restaurants/{id}`) — 인증 불필요, 공유 링크 대응
- 외부 지도 링크 조회 (`GET /api/v1/restaurants/{id}/navigation-links`) — 네이버·카카오
- Google Places 연동: `GooglePlacesClient`(Nearby Search, 카테고리별 `includedTypes` 선처리), Google Place ID 기준 upsert·갱신 정책, primaryType → 5개 음식 카테고리 매핑
- PostGIS `geography(Point, 4326)` 기반 5km 반경 후보 조회 (`RestaurantRepository.findWithinRadius`, GiST 인덱스)
- 데이트·친구·가족·혼밥·회식 적합도 3상태(`true`/`false`/`NULL`, 큐레이션 전용 — Google 갱신이 건드리지 않음)

**M2에서 결정해야 할 것** (착수 전 확정 필요):
- 평점 보정 공식·거리 가중치, 동행 적합 가산점 수치
- 추천 후보 조회 방식: 매 요청 실시간 Google 호출 vs 우리 DB 누적 조회 vs 하이브리드

Pick, 후기·피드, 탐색 스팟은 M2 이후 작업입니다.

제품 범위와 데이터 원본 결정, 결정 이력은 [`docs/SERVICE_DECISIONS.md`](docs/SERVICE_DECISIONS.md)에 기록합니다.

## 브랜치 / PR 전략

작업 단위마다 `feature/*` 브랜치를 새로 만들고 PR로 병합합니다. 자세한 컨벤션은 Notion의 [Git Branch Naming Convention](https://app.notion.com/p/d9f482bdb23e823da7da019c93d5c3ee), [Git Commit Message Convention](https://app.notion.com/p/b61482bdb23e82a5b8de010191af9500) 문서를 참고하세요.

## 문서

- 코드/네이밍 컨벤션, ERD, API 명세 등은 Notion "PickEat 프로젝트 > 백엔드" 페이지에 정리되어 있습니다.
