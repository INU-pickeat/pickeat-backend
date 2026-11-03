# API Conventions

API URL, 인증, 응답, 오류 규칙이다. 개별 API 명세는 Notion BE > API SPECIFICATION이 원본이고, 로컬에서는 Swagger UI(`/swagger-ui.html`, `local` 프로필만)로 확인한다.

## URL

- 서비스 API는 `/api/v1`로 시작한다. 관리자 API만 `/api/admin`을 쓴다
- 자원은 복수형 명사로 쓴다 (`/recommendations`, `/picks`, `/reviews`)
- 로그인한 회원 본인의 자원은 `/me` 아래에 둔다 (`/api/v1/me/picks`)
- 자원에 대한 동작은 하위 자원으로 표현한다 (`POST /recommendations/{sessionId}/exclusions`, `POST /reviews/{reviewId}/likes`)

## 인증

- 인증이 필요한 API는 `Authorization: Bearer <access token>` 헤더를 받는다
- 회원 식별은 access token(JWT)의 subject로만 한다. 요청 본문이나 경로로 회원 ID를 받지 않는다
- 인증 없이 열 API는 `SecurityConfig`의 `permitAll` 목록에 추가한다

## 요청·응답

- 요청·응답 본문은 JSON이고 필드는 camelCase다
- 날짜·시간은 `Instant`로 응답한다(ISO 8601 UTC). "오늘", "이번 달"처럼 날짜 경계가 필요한 계산은 한국 시간(`Asia/Seoul`) 기준이다
- 생성은 201, 본문 없는 성공은 204를 쓴다

## 오류

모든 오류는 같은 형식으로 응답한다.

```json
{ "code": "PICK_001", "message": "존재하지 않는 Pick입니다." }
```

- 비즈니스 오류는 `throw new BusinessException(PickErrorCode.PICK_NOT_FOUND)`처럼 던지고, `GlobalExceptionHandler`가 위 형식으로 바꾼다. 도메인별 예외 클래스는 만들지 않는다
- 오류 코드는 도메인 enum `{도메인}ErrorCode`(`BaseErrorCode` 구현)에 둔다. 상수 이름은 상황을 드러내고(`PICK_NOT_FOUND`), 응답 `code`는 `{도메인}_{3자리 번호}`다
- 새 오류는 그 도메인의 마지막 번호 다음을 쓴다. 이미 쓴 번호의 의미는 바꾸지 않는다(프론트가 `code`로 분기한다)
- 공통 오류는 `GlobalErrorCode`를 쓴다: 입력 오류·쿼리 파라미터 누락 `GLOBAL_001`, 인증 없음 `GLOBAL_002`, 경로 없음 `GLOBAL_003`, 메서드 불가 `GLOBAL_004`, 예상하지 못한 오류 `GLOBAL_500`
- `message`는 사용자에게 그대로 보여줄 수 있는 한국어 문장이다
