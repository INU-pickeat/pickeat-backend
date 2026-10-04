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
| `JWT_SECRET` | 필수 (HS256 기준 32바이트 이상) |
| `JWT_ACCESS_TOKEN_VALIDITY_MS` | `1800000` (30분) |
| `GOOGLE_PLACES_API_KEY` | Google Places Nearby Search API 키 |
| `REVIEW_IMAGE_BUCKET` | 후기 이미지 S3 버킷. 비우면 이미지 업로드 URL 발급만 꺼진다 |
| `REVIEW_IMAGE_REGION` | `ap-northeast-2` |
| `REVIEW_IMAGE_BASE_URL` | 이미지 제공 주소(CloudFront 등). 비우면 S3 버킷 주소 |

로컬에 `pick_eat`라는 이름의 데이터베이스를 미리 만들어두면 됩니다. 스키마는 Flyway 마이그레이션(`src/main/resources/db/migration`)이 기동 시 자동으로 적용합니다.

### 2. 서버 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Swagger UI는 `local` 프로필에서만 활성화됩니다. API 문서 없이 기본 프로필로 실행하려면 `./gradlew bootRun`을 사용합니다.

### 3. API 테스트 (Swagger UI)

서버가 뜨면 `http://localhost:8080/swagger-ui/index.html` 에서 API를 바로 테스트할 수 있습니다.

1. `POST /api/v1/auth/signup`으로 회원가입
2. `POST /api/v1/auth/login`으로 로그인 후 `accessToken`·`refreshToken` 발급
3. 우측 상단 `Authorize` 버튼에 `Bearer <accessToken>` 입력
4. 인증이 필요한 API 호출

## 테스트

```bash
./gradlew test
```

통합 테스트는 로컬 `pick_eat` 데이터베이스에 Flyway 마이그레이션을 적용합니다. PostgreSQL에
PostGIS가 설치되어 있어야 하며, CI에서는 별도의 PostGIS 서비스 데이터베이스를 사용합니다.

## 현재 구현 범위

**M1 Restaurant, M2 Recommendation, M3 Pick 마일스톤 완료, 운영 배포 완료(`https://api.pickeat.kr`). Phase 2의 Review·공개 피드·좋아요·후기 요약 구현.**

- Member 회원가입·로그인과 JWT 인증 (`POST /api/v1/auth/signup`, `/login`)
- Refresh Token 재발급·로그아웃 (`POST /api/v1/auth/refresh`, `/logout`) — 14일, 사용 시 새 토큰으로 교체(rotation), DB에는 해시만 저장
- 내 프로필 조회·수정 API (`GET`/`PATCH /api/v1/me`) — 닉네임·자기소개·프로필 이미지 URL 부분 수정
- 식당 상세 조회 (`GET /api/v1/restaurants/{id}`) — 인증 불필요, 공유 링크 대응
- 외부 지도 링크 조회 (`GET /api/v1/restaurants/{id}/navigation-links`) — 네이버·카카오
- Google Places 연동: `GooglePlacesClient`(Nearby Search, 카테고리별 `includedPrimaryTypes` 선처리), Google Place ID 기준 upsert·갱신 정책, primaryType → 7개 음식 카테고리 매핑
- PostGIS `geography(Point, 4326)` 기반 반경 후보 조회(수도권·부산 1km · 그 외 5km) (`RestaurantRepository.findWithinRadius`, GiST 인덱스)
- 데이트·가족·아이·혼자·단체·반려견 적합도 3상태(`true`/`false`/`NULL`, 큐레이션 전용 — Google 갱신이 건드리지 않음)
- M2 ADR 확정, 추천 요청 DTO(`RecommendationRequest`), 점수 계산기(`RecommendationScoreCalculator`), 세션·후보 스키마(V7 마이그레이션)
- 추천 생성·세션 조회 API (`POST`/`GET /api/v1/recommendations`) — DB 우선, 부족할 때만 Google 호출·upsert 하이브리드 조회. 선택적 가격대 필터(`priceRange`)를 지원하며 상위 10개를 세션 후보로 저장하고 상위 5개만 응답한다
- 재추천(제외) API (`POST /api/v1/recommendations/{sessionId}/exclusions`) — 식당을 제외 사유와 함께 제외하면 세션에 저장해 둔 6~10위 대체 후보로 그 자리를 채워 다시 상위 5개를 반환한다. 제외는 해당 세션 안에서만 유효하다
- Pick 생성·상태 변경 API (`POST /api/v1/picks`, `PATCH /api/v1/picks/{pickId}`) — 추천 세션에 실제 노출된 후보만 선택 가능. 생성 시 추천 세션의 동행 유형을 스냅샷으로 저장. 상태 변경 API로는 취소(`CANCELED`)만 할 수 있고, `REVIEWED`는 후기 작성으로만 전환된다
- 내 최근 Pick 목록 API (`GET /api/v1/me/picks?period=week|month`) — SELECTED + REVIEWED를 식당별로 그룹화해 `pickCount`·`latestPickedAt` 반환
- 내 Pick 지도 API (`GET /api/v1/me/picks/map`) — 본인 데이터만 조회, REVIEWED만 노출(SELECTED·CANCELED 미노출)
- 내 Pick 캘린더 API (`GET /api/v1/me/picks/calendar?year=2026&month=9`) — REVIEWED만 방문일(`visitedAt`, 한국 시간) 기준 날짜별로 묶어 `recordCount`·그날 첫 식당 이름 반환. `representativeImageUrl`은 그날 사진이 있는 첫 후기의 첫 번째 사진이며 없으면 `null`
- 후기 작성·조회·수정·삭제 API (`POST /api/v1/reviews`, `GET`/`PATCH`/`DELETE /api/v1/reviews/{reviewId}`) — Pick 하나당 후기 하나. 별점·내용·음식 카테고리·동행 유형·공개 범위(`PUBLIC`/`PRIVATE`)·이미지 최대 5장. 작성하면 Pick이 REVIEWED가 되고, 삭제하면 SELECTED로 돌아간다
- 후기 이미지 업로드 URL API (`POST /api/v1/reviews/images/upload-urls`) — S3 presigned PUT URL 발급. 버킷이 설정되지 않은 환경에서는 `REVIEW_006`(503)
- 공개 피드 API (`GET /api/v1/feed?cursor&size`) — 공개 후기만 최신순 커서 조회, 항목마다 좋아요 수와 내 좋아요 여부 포함
- 후기 좋아요·취소 API (`POST`/`DELETE /api/v1/reviews/{reviewId}/likes`) — 공개 후기에만 가능
- 식당별 후기 요약 API (`GET /api/v1/restaurants/{id}/review-summary`) — 인증 불필요. 공개 후기 수·평균 별점·대표 한줄평
- 탐색 스팟 한줄평(`oneLineIntro`)은 앱 내 사용자 후기 기반 — 식당별 가장 최근 공개 후기의 첫 줄(최대 50자), 후기가 없으면 `"후기가 없습니다."`
- 초기 탐색 스팟 조회 API (`GET /api/v1/discovery-spots`) — 인증 없이 신사·혜화·서촌·한남·종로와 지역별 고정 노출 식당을 순서대로 조회

**다음 할 일:** 후기 이미지용 S3 버킷·IAM 역할 생성과 운영 환경변수 설정, 프론트엔드 연동 E2E, CD 자동 롤백 실검증이 남아 있습니다. 기능으로는 프로필 이미지 업로드, 후기 한줄평 선정 규칙 확정, 데이트 프랜차이즈 제외, 제네릭 `restaurant` 분류가 남아 있고, 동적 지역별 인기맛집은 후기·좋아요 데이터가 쌓인 뒤 진행합니다.

초기 탐색 스팟은 신사·혜화·서촌·한남·종로 5개 지역과 지역별 5곳(총 25곳)의 운영자 선정 `CURATED` 데이터입니다. 실제 Picker 행동 데이터가 쌓이기 전까지 자동 인기 집계나 실시간 순위는 구현하지 않습니다.

이 고정 25곳은 탐색 화면용이며 M2 위치 기반 추천의 후보 정책을 대신하지 않습니다. 이후 Pick, 후기·피드와 행동 데이터가 충분해지면 동적 인기맛집으로 확장합니다.

가짜 식당과 임의 좌표는 운영 DB용 Flyway 시드에 넣지 않습니다. 확정 목록은 [`src/main/resources/curated/discovery-spots.csv`](src/main/resources/curated/discovery-spots.csv)에 정리했고, Google Places에서 검증한 좌표·Place ID로 V14 실제 탐색 시드를 작성했습니다.

## 향후 작업 순서

1. 후기 이미지 S3 버킷·IAM 역할·CORS 설정 (`docs/DEPLOYMENT.md` 참고)
2. 프론트엔드 연동 E2E 검증 (API Base URL: `https://api.pickeat.kr`)
3. CD 자동 롤백 실검증
4. 프로필 이미지 업로드 (후기 이미지와 같은 presigned URL 방식)
5. 후기 한줄평 선정 규칙 확정
6. 제네릭 `restaurant` 보완 분류, 데이트 프랜차이즈 제외 (브랜드 목록 준비 전까지 보류)
7. 동적 지역별 인기맛집 (후기·좋아요 데이터가 쌓인 뒤)

추천 세션당 Pick은 하나만 허용하고, 다른 세션에서는 같은 식당을 다시 선택할 수 있습니다. 지도와 캘린더는 후기를 작성한(REVIEWED) Pick만 보여 줍니다.

CI는 `.github/workflows/ci.yml`, CD는 `.github/workflows/deploy.yml`에서 실행합니다. `main`의 CI가 성공하면 EC2에 실행 JAR를 배포하고 헬스체크 실패 시 직전 릴리스로 롤백합니다. 운영은 EC2 t3.micro 한 대에 앱(systemd)·PostGIS(Docker)·Nginx를 함께 올린 구성이며, 2026-10-04부터 `https://api.pickeat.kr`로 서비스합니다.

EC2 최초 설정, GitHub Secrets, systemd, 헬스체크와 롤백 절차는 [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md)를 참고하세요.

제품 범위와 데이터 원본 결정, 결정 이력은 [`docs/SERVICE_DECISIONS.md`](docs/SERVICE_DECISIONS.md)에 기록합니다.

## 브랜치 / PR 전략

작업 단위마다 `feature/*` 브랜치를 새로 만들고 PR로 병합합니다. 자세한 컨벤션은 Notion의 [Git Branch Naming Convention](https://app.notion.com/p/d9f482bdb23e823da7da019c93d5c3ee), [Git Commit Message Convention](https://app.notion.com/p/b61482bdb23e82a5b8de010191af9500) 문서를 참고하세요.

## 문서

- 코드/네이밍 컨벤션, ERD, API 명세 등은 Notion "PickEat 프로젝트 > 백엔드" 페이지에 정리되어 있습니다.
