# 회원·인증

코드: `src/main/java/com/pickeat/pickeatbackend/domain/member/`. 이 도메인의 현재 규칙과 결정 기록이다. 진행 현황은 [progress.md](../progress.md), API 명세 원본은 Notion BE > API SPECIFICATION이다.

## 결정 기록 (최신순)

구현 당시의 판단과 결과다. 현재 규칙과 다르면 현재 규칙이 우선이다.

### 비밀번호 찾기 구현 결과 (2026-10-12)

- 흐름은 `POST /api/v1/auth/password-reset/email-verifications`(발송) → 기존 `POST /api/v1/auth/email-verifications/confirm`(확인) → `POST /api/v1/auth/password-reset`(`{email, newPassword}`)이다. 모두 인증 없이 호출하며 성공 시 204를 반환한다.
- 가입용 인증과 같은 `email_verifications` 테이블과 규칙(유효 10분, 재발송 1분, 실패 5회, 확인 후 30분)을 쓴다. 가입용은 미가입 이메일, 재설정용은 가입한 이메일에만 발송하므로 한 테이블에서 섞이지 않아 마이그레이션이 없다.
- 비밀번호를 바꾸면 그 회원의 refresh token을 모두 삭제해 모든 기기에서 다시 로그인하게 한다. 이미 발급된 access token은 만료(30분)까지 유효하다.
- 오류 코드: 가입하지 않은 이메일 `MEMBER_003`(404), 확인하지 않고 재설정 `MEMBER_005`(403), 새 비밀번호 8~64자 위반 `GLOBAL_001`(400). 인증번호 관련 오류는 가입 인증과 같다. 가입 API가 이미 `MEMBER_001`로 가입 여부를 알려 주므로 이메일 존재 여부를 숨기지 않았다.

### 프로필 이미지 업로드 (2026-10-08)

- `POST /api/v1/me/profile-image/upload-url` `{ "contentType": "image/jpeg" }` → `{ uploadUrl, imageUrl, contentType, expiresAt }`. 클라이언트가 `uploadUrl`에 같은 `Content-Type`으로 PUT한 뒤 `imageUrl`을 `PATCH /api/v1/me`의 `profileImageUrl`로 보낸다.
- 후기 이미지와 같은 버킷·같은 서명 방식(10분 유효, JPEG·PNG·WebP)을 쓰고 경로만 `profiles/{memberId}/`로 나눈다. 별도 저장소를 두지 않은 이유는 버킷·CloudFront·CORS 설정을 한 번으로 끝내기 위해서다. 오류 코드도 후기 이미지와 같다(400 `REVIEW_005`, 503 `REVIEW_006`).
- 프로필 경로의 이미지는 후기에 붙일 수 없다(후기 첨부는 `reviews/{memberId}/`만 허용).
- `PATCH /api/v1/me`의 `profileImageUrl`은 기존처럼 http(s) URL이면 받는다. 업로드 URL로 올린 이미지만 받도록 좁히는 것은 프론트 연동이 끝난 뒤 결정한다.

### 이메일 인증 구현 결과 (2026-10-07)

- 흐름은 `POST /api/v1/auth/email-verifications`(발송) → `POST /api/v1/auth/email-verifications/confirm`(확인) → 기존 `POST /api/v1/auth/signup`이다. 회원가입 요청 본문은 바꾸지 않았다. 두 인증 API는 인증 없이 호출하며 성공 시 204를 반환한다.
- 인증번호는 `SecureRandom` 6자리 숫자이고 DB(`email_verifications`, V20)에는 BCrypt 해시만 저장한다. 유효 10분, 재발송 1분 제한, 실패 5회 초과 시 거부, 인증 후 30분 안에 가입해야 한다. 가입에 성공하면 인증 정보를 삭제해 재사용을 막는다.
- 이메일은 발송·확인·가입·로그인 모두 `strip()` + 소문자로 정규화한다.
- 오류 코드: `MEMBER_005`(403, 미인증), `MEMBER_006`(400, 잘못된 번호), `MEMBER_007`(410, 만료), `MEMBER_008`(429, 재발송 제한), `MEMBER_009`(503, 메일 발송 실패). 이미 가입된 이메일은 기존 `MEMBER_001`(409).
- 발신 계정은 Gmail SMTP(`cki08543@gmail.com`)다. 운영 서버 `/etc/pickeat/pickeat.env`의 `MAIL_PASSWORD`에 앱 비밀번호(영문 소문자 16자, 공백 없이)를 넣었고, 2026-10-07 운영에서 발송·수신·확인을 검증했다.
- 이미 인증을 마친 이메일은 확인 API에 어떤 번호를 보내도 204를 반환한다(인증 완료 여부를 번호 비교보다 먼저 확인). 이미 인증된 상태라 보안 영향은 없다.

### Member 프로필 구현 결과 (2026-09-28)

- `members.bio VARCHAR(150)` 컬럼을 추가했다(V16). 기획서에 길이 제한이 없어 150자로 정한 구현 판단이며, 기획 확정 시 조정한다.
- `GET /api/v1/me`는 `memberId`·`email`·`nickname`·`bio`·`profileImageUrl`을 반환한다. 비밀번호는 응답에 포함하지 않는다.
- `PATCH /api/v1/me`는 부분 수정이다. 생략(`null`)한 필드는 그대로 두고, `bio`·`profileImageUrl`은 빈 문자열이면 삭제한다. `nickname`은 앞뒤 공백을 제거한 뒤 검증하며, 한글 완성형·영문·숫자만 최대 10자까지 허용한다(공백·특수문자·이모지·자모 불가, 회원가입도 동일). DB 컬럼도 `VARCHAR(10)`으로 줄였다(V17).
- 프로필 이미지는 Object Storage(S3 여부 미확정)가 아직 없어 업로드 API 없이 `http(s)` URL 문자열만 받는다. 저장소가 확정되면 리뷰 이미지와 같은 업로드 흐름을 붙인다.
- 토큰의 회원이 없으면 `MEMBER_003`(404)으로 응답한다.

### Refresh Token 구현 결과 (2026-09-28)

- 로그인 응답에 `refreshToken`을 추가했다. 형식은 32바이트 무작위 값의 Base64URL 문자열이며 JWT가 아니다. 유효기간은 14일(`JWT_REFRESH_TOKEN_VALIDITY_MS`, 기본 1209600000)이고 access token은 기존대로 30분이다.
- DB(`refresh_tokens`, V18)에는 원문 대신 SHA-256 해시만 저장한다. 로그인한 기기마다 행이 하나씩 생겨 기기별로 따로 로그아웃된다. 회원이 삭제되면 함께 지운다.
- `POST /api/v1/auth/refresh`는 refresh token을 받아 새 access·refresh token 쌍을 돌려준다. 쓴 토큰은 바로 지운다(rotation). 없거나 이미 쓴 토큰, 만료된 토큰은 모두 `MEMBER_004`(401)이다. 같은 토큰으로 동시에 요청하면 삭제 행 수로 판별해 한 요청만 성공한다.
- `POST /api/v1/auth/logout`은 refresh token을 폐기하고 204를 돌려준다. access token이 만료된 뒤에도 로그아웃할 수 있도록 인증 없이 호출하며, 이미 없는 토큰이어도 204다. 이미 발급된 access token은 만료(최대 30분)까지 유효하다.
- 쿠키를 쓰지 않는 기존 방식(Authorization 헤더, CORS credentials 불허)을 유지해 토큰은 요청·응답 body로 주고받는다.
- 만료된 토큰 행은 같은 회원이 새로 발급받을 때 정리한다. 별도 정리 배치는 두지 않는다.
- 재사용 감지(이미 쓴 토큰이 다시 오면 그 계열 토큰 전체 폐기)는 구현하지 않았다. 탈취 대응이 필요해지면 `family_id` 컬럼을 추가한다.
