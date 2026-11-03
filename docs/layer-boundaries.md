# Layer Boundaries

Controller, Service, Repository, Entity, DTO의 책임과 네이밍 규칙이다.

## 계층별 책임

| 계층 | 책임 |
| --- | --- |
| Controller | HTTP 요청·응답, `@Valid` DTO 검증, 인증 회원 ID 전달, 상태 코드. 상태·헤더를 직접 제어할 때는 `ResponseEntity`를 쓰고, 단순 200 응답은 DTO를 직접 반환해도 된다 |
| Service | 비즈니스 규칙, 권한 확인, Entity 조회·변경, 트랜잭션 |
| Repository | 데이터 접근의 기본 위치. JPQL·네이티브 쿼리도 여기에 둔다. 읽기 전용 리포팅 집계의 `JdbcTemplate` 예외는 [persistence.md](persistence.md#연관관계)를 따른다 |

Entity 작성과 트랜잭션 규칙은 [persistence.md](persistence.md)를 따른다.

## 검증 위치

| 위치 | 검증 내용 |
| --- | --- |
| DTO | 필수값, 길이, 범위, 형식 등 요청 자체 (Bean Validation) |
| Service | 권한, 중복, 존재 여부, 상태 전이처럼 DB 조회가 필요한 규칙 |
| Entity | 항상 지켜져야 하는 상태와 불변식 |
| DB | NOT NULL, UNIQUE, CHECK 제약으로 한 번 더 막는다 |

## DTO

- DTO는 Java `record`로 쓰고 도메인의 `dto/` 패키지에 둔다. 요청 값을 해석하는 작은 enum(`PickPeriod` 등)도 같은 곳에 둔다
- Entity를 API 요청·응답으로 직접 노출하지 않는다
- 요청 DTO와 응답 DTO를 나눈다
- Entity나 조회 결과 하나를 응답으로 바꿀 때는 `from()`, 여러 값이나 부가 정보를 조합할 때는 `of()` 같은 DTO 정적 메서드를 쓴다
- 목록 응답은 배열을 바로 내리지 않고 `items` 등을 담은 응답 DTO로 감싼다 (`RecentPicksResponse`, `FeedResponse`)

## 공통 작성 규칙

- 생성자 주입에는 `@RequiredArgsConstructor`를 쓴다
- 메서드 이름은 수행하는 동작을 구체적으로 드러낸다
- 들여쓰기 4칸을 쓰고, 긴 줄은 읽기 쉬운 단위로 나눈다. import 와일드카드(`*`)는 쓰지 않는다
- 주석은 한국어로, 코드만 봐서는 알 수 없는 이유와 제약을 적는다

## 클래스 네이밍

- PascalCase 단수형을 쓰고 계층 역할은 접미사로 드러낸다
- 약어도 첫 글자만 대문자로 쓴다 (`JwtProvider` ○ / `JWTProvider` ✕)

| 종류 | 형식 | 예시 |
| --- | --- | --- |
| Controller | `{도메인}Controller` | `PickController`, `AdminStatsController` |
| Service | `{도메인}Service`. 책임이 뚜렷하게 다른 기능만 별도 Service로 나눈다 | `PickService`, `ReviewSummaryService` |
| Repository | `{Entity}Repository` | `PickRepository` |
| 요청 DTO | 동작과 대상을 드러내는 `{설명}Request` | `CreatePickRequest`, `PickStatusUpdateRequest` |
| 응답 DTO | `{도메인}Response`, 용도가 다르면 `{용도}Response` | `PickResponse`, `RecentPicksResponse` |
| ErrorCode | `{도메인}ErrorCode` enum | `PickErrorCode` |
| 외부 API | `{대상}Client` | `GooglePlacesClient` |
| Enum | PascalCase 단수형, 값은 UPPER_SNAKE_CASE | `CompanionType.DATE` |

- 인터페이스와 구현체는 구현체가 둘 이상이거나, S3 같은 외부 인프라 경계를 분리해 테스트 대역으로 바꿔야 할 때만 나눈다
- 쿼리 메서드명은 조회 조건이 드러나게 쓴다
- 한 클래스에서만 쓰는 상수는 그 클래스의 `private static final`로 두고, 환경마다 달라지는 값은 `application.yaml`에 `${ENV:기본값}`으로 둔다
