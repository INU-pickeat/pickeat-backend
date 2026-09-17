# Pick Eat Backend

PickEat 프로젝트의 백엔드 서버입니다.

## 기술 스택

- Java 21, Spring Boot 4.1.1
- Spring Data JPA, Flyway (PostgreSQL)
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

1. `POST /api/users/signup`으로 회원가입
2. `POST /api/users/login`으로 로그인 후 `accessToken` 발급
3. 우측 상단 `Authorize` 버튼에 `Bearer <accessToken>` 입력
4. 인증이 필요한 API 호출

## 테스트

```bash
./gradlew test
```

## 브랜치 / PR 전략

작업 단위마다 `feature/*` 브랜치를 새로 만들고 PR로 병합합니다. 자세한 컨벤션은 Notion의 [Git Branch Naming Convention](https://app.notion.com/p/d9f482bdb23e823da7da019c93d5c3ee), [Git Commit Message Convention](https://app.notion.com/p/b61482bdb23e82a5b8de010191af9500) 문서를 참고하세요.

## 문서

- 코드/네이밍 컨벤션, ERD, API 명세 등은 Notion "PickEat 프로젝트 > 백엔드" 페이지에 정리되어 있습니다.
