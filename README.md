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
| `GOOGLE_PLACES_API_KEY` | Google Places Nearby Search API 키 |

로컬에 `pick_eat`라는 이름의 데이터베이스를 미리 만들어두면 됩니다. 스키마는 Flyway 마이그레이션(`src/main/resources/db/migration`)이 기동 시 자동으로 적용합니다.

### 2. 서버 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Swagger UI는 `local` 프로필에서만 활성화됩니다. API 문서 없이 기본 프로필로 실행하려면 `./gradlew bootRun`을 사용합니다.

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

**M1 Restaurant, M2 Recommendation, M3 Pick 마일스톤 완료.**

- Member 회원가입·로그인과 JWT 인증 (`POST /api/v1/auth/signup`, `/login`)
- 식당 상세 조회 (`GET /api/v1/restaurants/{id}`) — 인증 불필요, 공유 링크 대응
- 외부 지도 링크 조회 (`GET /api/v1/restaurants/{id}/navigation-links`) — 네이버·카카오
- Google Places 연동: `GooglePlacesClient`(Nearby Search, 카테고리별 `includedTypes` 선처리), Google Place ID 기준 upsert·갱신 정책, primaryType → 5개 음식 카테고리 매핑
- PostGIS `geography(Point, 4326)` 기반 5km 반경 후보 조회 (`RestaurantRepository.findWithinRadius`, GiST 인덱스)
- 데이트·친구·가족·혼밥·회식 적합도 3상태(`true`/`false`/`NULL`, 큐레이션 전용 — Google 갱신이 건드리지 않음)
- M2 ADR 확정, 추천 요청 DTO(`RecommendationRequest`), 점수 계산기(`RecommendationScoreCalculator`), 세션·후보 스키마(V7 마이그레이션)
- Top 5 추천 생성·세션 조회 API (`POST`/`GET /api/v1/recommendations`) — 실시간 Google 호출 + upsert 후 점수순 정렬(ADR-M2-2 Option A)
- Pick 생성·상태 변경 API (`POST /api/v1/picks`, `PATCH /api/v1/picks/{pickId}`) — 추천 세션에 실제 노출된 후보만 선택 가능
- 내 Pick 목록·지도 API (`GET /api/v1/me/picks`, `GET /api/v1/me/picks/map`) — 본인 데이터만 조회, 취소 Pick은 지도에서 제외

**다음 할 일 (M2.5):** 초기 탐색 스팟 — 5개 지역 코드, 25개 식당·노출 순서 DB 시드, 탐색 조회 API. 다만 실제 식당 목록은 기획자가 전달할 ZIP 파일을 기준으로 해야 하므로 현재 데이터 확정 전까지 구현을 보류합니다. 진행 순서와 결정 내용은 [`docs/SERVICE_DECISIONS.md`](docs/SERVICE_DECISIONS.md)의 "구현 현황"을 참고하세요.

M2 완료 직후에는 초기 탐색 스팟을 구현합니다. 성수동·연남동·신사동·서촌·을지로3가 5개 지역과 지역별 5곳(총 25곳)을 운영자 선정 `CURATED` 데이터로 DB에 직접 시드합니다. 실제 Picker 행동 데이터가 쌓이기 전까지 자동 인기 집계나 실시간 순위는 구현하지 않습니다.

이 고정 25곳은 탐색 화면용이며 M2 위치 기반 추천의 후보 정책을 대신하지 않습니다. 이후 Pick, 후기·피드와 행동 데이터가 충분해지면 동적 인기맛집으로 확장합니다.

가짜 식당은 운영 DB용 Flyway 시드에 넣지 않습니다. 탐색 API 개발이나 프론트 계약 검증에 임시 데이터가 필요하면 테스트 fixture 또는 API mock으로만 사용하고, 실제 탐색 시드는 기획 데이터가 확정된 뒤 V9 이후 마이그레이션으로 작성합니다.

## 향후 작업 순서

1. 기획 ZIP 기준 25개 식당 확정
2. M2.5 초기 탐색 스팟 구현
3. AWS EC2 배포와 GitHub Actions CD 구축
4. 프론트엔드 연동 E2E 검증

M3 Pick은 완료됐습니다. 추천 세션당 Pick은 하나만 허용하고, 다른 세션에서는 같은 식당을 다시 선택할 수 있습니다. 상태는 `SELECTED`에서 `REVIEWED` 또는 `CANCELED`로만 전환하며 종료 상태는 되돌리지 않습니다.

CI는 `.github/workflows/ci.yml`에서 실행 중입니다. `.github/workflows/deploy.yml`에는 CI 성공 후 EC2에 실행 JAR를 배포하고 헬스체크 실패 시 직전 릴리스로 롤백하는 CD가 준비되어 있습니다. 실제 배포는 EC2와 운영 DB를 만든 뒤 GitHub 저장소 변수 `DEPLOY_ENABLED=true`를 설정해야 활성화됩니다.

EC2 최초 설정, GitHub Secrets, systemd, 헬스체크와 롤백 절차는 [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md)를 참고하세요.

제품 범위와 데이터 원본 결정, 결정 이력은 [`docs/SERVICE_DECISIONS.md`](docs/SERVICE_DECISIONS.md)에 기록합니다.

## 브랜치 / PR 전략

작업 단위마다 `feature/*` 브랜치를 새로 만들고 PR로 병합합니다. 자세한 컨벤션은 Notion의 [Git Branch Naming Convention](https://app.notion.com/p/d9f482bdb23e823da7da019c93d5c3ee), [Git Commit Message Convention](https://app.notion.com/p/b61482bdb23e82a5b8de010191af9500) 문서를 참고하세요.

## 문서

- 코드/네이밍 컨벤션, ERD, API 명세 등은 Notion "PickEat 프로젝트 > 백엔드" 페이지에 정리되어 있습니다.
