# 후기·피드

코드: `src/main/java/com/pickeat/pickeatbackend/domain/review/`. 이 도메인의 현재 규칙과 결정 기록이다. 진행 현황은 [progress.md](../progress.md), API 명세 원본은 Notion BE > API SPECIFICATION이다.

## 결정 기록 (최신순)

구현 당시의 판단과 결과다. 현재 규칙과 다르면 현재 규칙이 우선이다.

### 후기 키워드·1인당 지출 구현 결과 (2026-11-03)

- 네이버 영수증 리뷰처럼 본문 없이도 후기를 남길 수 있게 했다. 사진(1~5장)과 "이런 점이 좋았어요" 키워드(1~5개)가 필수, 본문과 1인당 지출은 선택이다.
- 키워드는 `ReviewKeyword` enum 17개(맛 → 매장 → 서비스 → 동행 순), 1인당 지출은 `SpendPerPerson` enum 4구간(1만원 미만, 1~2만원, 2~3만원, 3만원 이상)이다. 응답은 둘 다 `{code, label}`이고 키워드는 enum 선언 순서로 정렬한다. 선택지 조회 API는 두지 않았다(프론트가 enum을 고정으로 가진다).
- 키워드 누락·null·빈 배열은 사진(`REVIEW_008`)과 같은 방식으로 요청 DTO에서 빈 목록으로 바꾸고 `ReviewService`가 `REVIEW_009`("좋았던 점을 1개 이상 골라 주세요.")로 거부한다. 6개 이상과 정의되지 않은 값은 `GLOBAL_001`이다. 요청은 `Set`으로 받아 중복이 하나로 합쳐지며 개수는 합친 뒤 기준이다.
- 수정(`PATCH`)에서 `keywords`를 보내면 통째로 교체하고 빈 배열은 `REVIEW_009`다. 본문은 빈 문자열·공백을 보내면 지운다(null 저장). 1인당 지출은 보낸 값으로만 바꾸며 지우는 방법은 두지 않았다.
- DB(V34): `reviews.content` NOT NULL 해제(공백 금지 CHECK는 NULL을 통과시켜 그대로 둠), `reviews.spend_per_person` + CHECK, `review_keywords(review_id, keyword)` 복합 PK·CHECK·`ON DELETE CASCADE`. 엔티티는 `@ElementCollection Set<ReviewKeyword>`(`RecommendationSession.foodCategories`와 같은 방식, 피드 N+1은 `@BatchSize`). 기존 후기는 키워드 없이 남는다.
- 식당 후기 요약에 `topKeywords`(공개 후기 기준 상위 5개, 건수 내림차순·같으면 enum 순서)를 추가했다.
- 한줄평 1단계(앱 사용자 후기)는 본문이 있는 가장 최근 공개 후기를 쓴다. 본문 없는 후기만 있으면 사용자 후기 한줄평이 없는 것으로 보고 기존 대체 규칙을 그대로 따른다.
- 미룬 일: 키워드로 `restaurants.suitable_for_*` 채우기, 1인당 지출로 가격대 채우기, 방문 횟수.
- **임시 조치(2026-11-03).** #76 배포 직후 키워드를 보내지 않는 기존 프론트의 후기 작성이 `REVIEW_009`로 실패할 수 있어, `ReviewService.KEYWORDS_REQUIRED`를 꺼서 키워드를 0~5개 선택으로 바꿨다. 프론트가 키워드 화면을 배포하면 true로 바꿔 다시 필수로 돌린다.

### Review·피드 구현 결과 (2026-10-04)

- **후기.** `POST /api/v1/reviews`, `GET`/`PATCH`/`DELETE /api/v1/reviews/{reviewId}`. 후기는 Pick 하나당 하나이며(`REVIEW_002`), 작성하면 그 Pick이 `REVIEWED`가 된다. 취소한 Pick에는 쓸 수 없다(`PICK_004`). 삭제하면 Pick이 `SELECTED`로 돌아가 지도·캘린더에서 빠지고 다시 후기를 쓸 수 있다.
- **입력값.** 내용 1~1000자, 음식 카테고리, 동행 유형, 공개 범위(`PUBLIC`/`PRIVATE`), 이미지 URL 1~5개 필수(`imageUrls` 배열, 순서가 표시 순서. 2026-10-10 1장에서 5장으로 늘렸고, 2026-10-21 최소 1장을 필수로 했다). 누락·`null`·빈 배열은 400 `REVIEW_008`("사진을 1장 이상 첨부해 주세요."), 6장 이상이나 빈 문자열 URL은 공통 입력 오류(`GLOBAL_001`)다. 필수화 이전 후기에는 사진이 없을 수 있다. 평점은 받거나 저장하지 않는다. 음식 카테고리와 동행 유형은 추천 세션 값이 아니라 후기 작성 화면에서 사용자가 직접 고른 값이다. 그래서 선택지 조회 API는 따로 만들지 않았다.
- **나의 기록.** `GET /api/v1/me/reviews?period=week|month`. Pick 시각(`selected_at`) 기준 최근 7일·30일 후기를 최신 Pick 순으로 반환하며 공개·비공개 후기를 모두 포함한다.
- **이미지.** `POST /api/v1/reviews/images/upload-urls`에 Content-Type 목록을 보내면 10분짜리 S3 presigned PUT URL과 저장될 `imageUrl`을 돌려준다. JPEG·PNG·WebP만 허용한다(`REVIEW_005`). 후기에는 본인 경로(`reviews/{memberId}/`) 아래의 URL만 붙일 수 있다(`REVIEW_004`). 버킷이 설정되지 않았으면 발급을 `REVIEW_006`(503)으로 거부한다. presigned PUT은 파일 크기를 제한하지 못하고, 후기에서 뺀 이미지의 S3 객체는 지우지 않는다.
- **공개 피드.** `GET /api/v1/feed?cursor&size`. `PUBLIC` 후기만 최신순으로 내려준다. 커서는 이전 응답의 `nextCursor`(마지막 `reviewId`)이고 `null`이면 마지막 페이지다. 크기는 1~50, 기본 20이다. 인증이 필요하며 항목마다 `likeCount`와 `likedByMe`를 준다. 항목마다 카드 썸네일 `thumbnailUrl`(후기 첫 사진)도 준다(2026-10-21). 사진 필수화 이전 후기는 사진이 없을 수 있어 `thumbnailUrl`은 nullable이고, 식당 이미지로 대체하지 않는다.
- **후기 사진 수정 제한.** 사진은 후기 작성 시에만 1~5장 등록할 수 있다. 후기 수정은 내용·음식 카테고리·동행 유형·공개 범위만 허용하며, `imageUrls`가 포함된 수정 요청은 400 `REVIEW_007`("이미지는 수정할 수 없습니다.")로 거부한다.
- **좋아요.** `POST`/`DELETE /api/v1/reviews/{reviewId}/likes`. 공개 후기에만 누를 수 있다. 본인의 비공개 후기는 `REVIEW_003`(409), 남의 비공개 후기는 존재를 알리지 않도록 `REVIEW_001`(404)이다. 여러 번 눌러도 한 번으로 세며, 동시 요청은 DB 유니크 제약과 `ON CONFLICT DO NOTHING`으로 처리한다.
- **식당별 후기 요약.** `GET /api/v1/restaurants/{restaurantId}/review-summary`. 공개 후기 수와 대표 한줄평을 돌려준다. 인증이 필요 없다. 후기가 없으면 0건·`"후기가 없습니다."`다.
- **한줄평.** 탐색 스팟의 `oneLineIntro`와 후기 요약의 `oneLineReview`는 같은 규칙을 쓴다. 식당별 가장 최근 공개 후기의 첫 줄이며 50자를 넘으면 잘라 말줄임표를 붙인다. 선정 규칙은 임시이며 [progress.md](../progress.md) 21번에서 확정한다. 탐색 스팟 `oneLineIntro`는 2026-10-21부터 공개 후기가 없으면 운영자 한줄평(`discovery_spot_restaurants.one_line_intro`, V32에서 23곳 갱신)을, 그것도 없으면 `"후기가 없습니다."`를 내려준다. 식당 상세(`GET /api/v1/restaurants/{id}`)의 `oneLineReview`도 같은 순서를 쓴다(앱의 "picker들의 한줄평"은 상세 화면에만 보이므로). 탐색 스팟이 아닌 식당은 운영자 한줄평이 없어 기존과 같다.
- **스키마.** V19에 `reviews`, `review_images`, `review_likes`를 추가했다. 이미지와 좋아요는 후기 삭제 시 함께 지워진다. V27에서 사용하지 않는 `reviews.rating` 컬럼과 제약을 제거했다.
