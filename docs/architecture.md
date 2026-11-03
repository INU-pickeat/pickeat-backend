# Architecture

## 시스템 구성

| 구성 요소 | 역할 |
| --- | --- |
| 프론트엔드 (Vercel, `www.pickeat.kr`) | 웹 클라이언트. CORS 허용 목록으로 API를 호출한다 |
| Spring Boot API (`api.pickeat.kr`) | Java 21, Spring Boot 4.1 단일 애플리케이션. EC2 t3.micro에서 systemd로 실행하고 Nginx가 앞에 있다 |
| PostgreSQL + PostGIS | 주 저장소. 같은 EC2의 Docker에서 실행하고 스키마는 Flyway로 관리한다 |
| Google Places API | DB 후보가 부족할 때 식당을 보충하고 사진을 가져온다 (`GooglePlacesClient`) |
| Amazon S3 | 후기·프로필 이미지. 클라이언트가 presigned PUT URL로 직접 올린다 |
| Gmail SMTP | 회원가입·비밀번호 찾기 인증번호 메일 |

배포 구성의 상세 내용은 [DEPLOYMENT.md](DEPLOYMENT.md)에 있다.

## 패키지 구조

기본 패키지는 `com.pickeat.pickeatbackend`이다. 도메인으로 먼저 나누고, 도메인 안을 계층으로 나눈다.

```plain text
global/
├── config/        // OpenAPI 등 공통 설정
├── exception/     // BaseErrorCode, GlobalErrorCode, BusinessException, ErrorResponse, GlobalExceptionHandler
└── security/      // SecurityConfig, jwt/

domain/
├── member/          // 회원, 인증, 이메일 인증, refresh token
├── restaurant/      // 식당, client/(Google Places), 식당 신고
├── recommendation/  // 추천 세션·후보·제외, 점수 계산
├── pick/            // Pick, 최근 Pick, 지도·캘린더
├── review/          // 후기, 피드, 좋아요, storage/(S3)
├── discovery/       // 초기 탐색 스팟
└── admin/           // 관리자 대시보드 집계
```

도메인 안의 하위 패키지는 필요한 것만 만든다.

```plain text
controller/  service/  repository/  entity/  dto/  exception/
client/      // 외부 API 호출
storage/     // 파일 저장소
```

- 패키지명은 영문 소문자 단수형이다
- 정적 페이지(`/admin.html`), 큐레이션 데이터(`curated/`), 탐색 스팟 이미지는 `src/main/resources` 아래에 둔다

## 의존 방향

```plain text
Controller → Service → Repository
```

- 의존은 한 방향으로만 흐른다. 계층별 책임은 [layer-boundaries.md](layer-boundaries.md)를 따른다
- 다른 도메인의 기능이 필요하면 그 도메인의 Service나 Repository를 주입받는다. Facade·UseCase 계층은 만들지 않는다

## 인증

회원 JWT, 관리자 HTTP Basic, 공개 경로 규칙의 정본은 [api-conventions.md](api-conventions.md#인증)다.
