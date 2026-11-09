# Pick

코드: `src/main/java/com/pickeat/pickeatbackend/domain/pick/`. 이 도메인의 현재 규칙과 결정 기록이다. 진행 현황은 [progress.md](../progress.md), API 명세 원본은 Notion BE > API SPECIFICATION이다.

## 결정 기록 (최신순)

구현 당시의 판단과 결과다. 현재 규칙과 다르면 현재 규칙이 우선이다.

### 홈 후기 작성 유도 배너 (2026-10-21)

- **조회.** `GET /api/v1/me/picks/review-prompt`. 항상 최대 1건. 조건: 상태 `SELECTED`(후기 미작성), `selectedAt >= 현재 - 7일`, 배너 미닫힘. 정렬 `selectedAt DESC, id DESC`의 첫 건을 `{ "pick": { pickId, restaurantId, restaurantName, foodCategory, representativeImageUrl, selectedAt } }`로 준다. 없으면 `{ "pick": null }`. `representativeImageUrl`은 최근 Pick 목록과 같은 규칙(자체 이미지 → 식당 사진 API 주소 → null)이다.
- **다음 Pick.** 최신 Pick을 기록하거나(`REVIEWED`) 카드를 지우거나(`CANCELED`) 닫으면 대상에서 빠지고, 조건을 만족하는 그다음 최근 Pick이 뜬다.
- **닫기.** `POST /api/v1/me/picks/{pickId}/review-prompt/dismiss` → 204. `picks.review_prompt_dismissed_at`(V31)에 남겨 기기가 바뀌어도 다시 뜨지 않는다. 여러 번 호출해도 204이고 처음 닫은 시각을 유지한다. 본인 Pick이 아니면 `PICK_001`(404). 닫아도 후기 작성과 최근 Pick 목록에는 영향이 없다.
- **연필 버튼.** 새 API 없이 `pickId`로 기존 후기 작성 화면(`POST /api/v1/reviews`)으로 보낸다.

### Pick 캘린더 구현 결과 (2026-10-04, 2026-10-10 개편)

- `GET /api/v1/me/picks/calendar?year&month`. Pick 시각(`selected_at`)을 한국 시간 날짜로 변환해 SELECTED·REVIEWED Pick의 `picks`를 반환한다. SELECTED 항목은 캘린더 하단의 후기 작성 진입점이며 후기를 작성하면 REVIEWED가 된다.
- 캘린더 날짜별 `dates`는 REVIEWED만 묶어 `recordCount`, 그날 첫 기록의 `restaurantName`, `representativeImageUrl`을 돌려준다. 후기를 쓰기 전 SELECTED Pick은 날짜 대표 이미지에 표시하지 않는다.
- 월 경계도 한국 시간 기준이다. 연 2000~2100·월 1~12 밖이거나 누락·숫자 아님이면 `GLOBAL_001`이다. 쿼리 파라미터 누락·타입 오류를 공통 예외 핸들러에서 `GLOBAL_001`로 응답하도록 함께 바꿨다.
- 대표 이미지는 그날 가장 먼저 쓴 후기 중 사진이 있는 첫 후기의 첫 번째(0번) 사진이다. 그날 후기에 사진이 하나도 없으면 `null`이다.

### M3 계약 개편 구현 결과 (2026-09-22)

- **Pick 동행 스냅샷.** `Pick`에 `companion_type` 컬럼을 추가하고(V12), 생성 시 추천 세션의 `companionType`을 그대로 복사해 저장한다. 이후 세션이 바뀌어도 이미 생성된 Pick의 값은 변하지 않는다. 기존 행은 연결된 세션의 현재 `companion_type`으로 백필했다.
- **Pick 지도 REVIEWED 전용.** 기존 "CANCELED만 제외"에서 "REVIEWED만 노출"로 바꿨다(`findByMemberIdAndStatusOrderBySelectedAtDescIdDesc`). `PickMapResponse.Item`에 스냅샷된 `companionType`과 후기 첫 사진인 `representativeImageUrl`을 담는다. 후기 사진이 없으면 `representativeImageUrl`은 `null`이다.
- **최근 Pick 목록 개편.** `GET /api/v1/me/picks`의 계약을 페이지 조회에서 `period=week|month` 필수 쿼리 파라미터 기반 식당별 집계로 완전히 교체했다(기존 `page`/`size`, `PickListResponse`는 제거). SELECTED + REVIEWED만 집계하고 CANCELED는 제외하며, 식당별로 `pickCount`와 `latestPickedAt`을 반환한다. `period`는 `week`(최근 7일)·`month`(최근 30일) 롤링 윈도우로 해석했다 — 기획서에 "일주일 기준"/"한 달 기준"의 정확한 경계(캘린더 월 vs 롤링 30일)가 명시되어 있지 않아 내린 구현 판단이며, 기획자 확인이 필요하면 조정한다. `period`가 `week`/`month`가 아니면 `GLOBAL_001`로 거부한다.
- **Pick 캘린더는 당시 범위 밖.** 대표 이미지 출처가 리뷰 사진인데 Review 모듈이 없어 이 작업에서는 구현하지 않았다. 2026-10-04에 구현했다 — "Pick 캘린더 구현 결과" 참고.
- 단위·API 계약·Flyway 통합 테스트를 갱신해 당시 기준 전체 테스트 184개가 통과했다.

### M3 Pick 구현 결과 (2026-09-21)

- 추천 세션당 최종 Pick은 하나만 허용한다. 같은 식당은 서로 다른 추천 세션에서 다시 Pick할 수 있다.
- Pick 대상은 요청한 회원이 소유한 추천 세션에 실제 후보로 저장된 식당이어야 한다. 다른 회원의 세션·Pick은 404로 처리해 존재 여부를 노출하지 않는다.
- 상태는 `SELECTED`, `REVIEWED`, `CANCELED` 세 가지다. `SELECTED`에서 `REVIEWED` 또는 `CANCELED`로만 전환하며, 같은 상태 요청은 멱등 처리하고 종료 상태는 되돌리지 않는다.
- 2026-10-04부터 `REVIEWED` 전환은 후기 작성(`POST /api/v1/reviews`)으로만 일어난다. `PATCH /api/v1/picks/{pickId}`로 `REVIEWED`를 요청하면 `PICK_005`(400)이고, 이 API로는 취소만 할 수 있다. 후기를 삭제하면 Pick은 `SELECTED`로 돌아간다.
- `REVIEWED` 전환 시 `visited_at`을 기록한다. (2026-09-22 개편으로 지도는 REVIEWED 전용, 목록은 기간별 집계로 바뀌었다 — "M3 계약 개편 구현 결과" 참고.)
- API는 `POST /api/v1/picks`, `PATCH /api/v1/picks/{pickId}`, `GET /api/v1/me/picks`, `GET /api/v1/me/picks/map`, `GET /api/v1/me/picks/calendar`(2026-10-04 추가) 다섯 개이며 모두 JWT 인증이 필요하다.
- Flyway V8에 `picks` 테이블, 추천 세션 유일 제약, 회원별 최신순·상태·식당 인덱스를 추가했다.
- 엔티티 상태 전이, 서비스 권한·중복·후보 검증, DB 제약, JWT 및 JSON 계약을 포함해 당시 기준 전체 테스트 134개가 통과했다.
