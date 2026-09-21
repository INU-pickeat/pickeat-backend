# Pick Eat 서비스 결정 사항

마지막 갱신일: 2026-09-21

## 제품 기준 문서

현재 Pick Eat 제품 기획이 기준(source of truth)이다. 예전 설문 서비스 문서와 과거 전시회 전용 식당 결정 사항은 역사적 참고용으로만 남겨둔다.

## 식당 데이터 전략

- 추천 후보는 Google Places 주변 검색(nearby search)에서 가져온다.
- 서비스는 안정적인 내부 ID, Pick, 후기, 외부 데이터 갱신에 필요한 최소한의 식당 스냅샷만 저장한다.
- `GOOGLE`과 `CURATED`는 `data_provider`로 구분해서 기록한다.
- 큐레이션 데이터는 초기 탐색 경험 전용이다: 성수동·연남동·신사동·서촌·을지로3가 각 5곳씩 총 25곳.
- Google 평점과 향후 Pick Eat 자체 평점은 의미와 갱신 정책이 달라서 분리해서 관리한다.

### 초기 인기맛집 탐색 스팟 고정 정책 (2026-09-21)

초기에는 실제 Picker와 Pick·좋아요·후기 데이터가 없고 실시간 인기 집계 기능도 구현하지 않는다. 따라서 탐색 스팟과 노출 식당은 자동 순위가 아니라 운영자가 선정한 고정 큐레이션으로 제공한다.

- 고정 지역: `성수동`, `연남동`, `신사동`, `서촌`, `을지로3가`.
- 지역별 식당은 정확히 5곳, 총 25곳으로 시작한다.
- 지역 목록은 초기 애플리케이션 계약에 고정하되 안정적인 지역 코드로 표현해서 이후 지역 추가가 가능하게 한다.
- 식당과 노출 순서는 소스 코드의 임시 객체가 아니라 Flyway 마이그레이션으로 DB에 직접 시드한다. 식당은 `data_provider=CURATED`로 구분한다.
- 동일 마이그레이션을 다시 적용해도 중복 식당이 생기지 않도록 안정적인 식별·중복 방지 기준을 함께 둔다.
- 초기 탐색 결과를 사용자 행동 기반의 "실시간 인기 순위"라고 표현하지 않는다. 현재 의미는 **운영자 선정 인기 후보**다.
- Pick·좋아요·후기 데이터와 집계 기능이 충분해지면 고정 목록을 동적 랭킹으로 교체한다. 그 전까지 자동 갱신·인기 점수·집계 배치는 구현하지 않는다.

이 25곳은 **탐색 스팟 전용 데이터**다. M2 위치 기반 추천의 후보 정책을 대신하지 않으며, 추천에서는 다른 Restaurant와 동일하게 5km·음식 카테고리 조건을 통과할 때만 후보가 될 수 있다.

### Google Nearby Search 카테고리 선처리 (2026-09-20)

`GooglePlacesClient.findNearbyRestaurants()`는 이제 `includedTypes`를 호출자가 넘긴다. 추천 요청에 카테고리가 있으면 `FoodCategory.toGooglePrimaryTypes()`(M1-2 매핑표의 역방향)로 구체적인 Google 하위 타입 목록을 만들어 넘기고, 카테고리가 없으면 `Set.of("restaurant")`로 광범위하게 요청한다.

- Google Nearby Search는 한 번 호출에 최대 20개까지만 반환하고, 이건 "반경 안 전체를 찾은 뒤 자르는" 게 아니라 Google이 자기 랭킹(`POPULARITY`/`DISTANCE`) 기준으로 골라주는 상위 20개다.
- `includedTypes`를 카테고리에 맞게 좁히면 이 20개 슬롯이 hotel·halal_restaurant처럼 어차피 제외할 타입에 낭비되지 않고 관련 있는 후보로 채워진다.
- 추천 후보를 매 요청마다 Google에 실시간으로 물어볼지, 우리 DB(M1-4 5km 조회)에 쌓인 데이터로 채울지는 아직 미정 — M2 구현 시 결정한다.

### Google upsert 정책 (M1-3, 2026-09-20)

- `google_place_id` 기준으로 조회 후 있으면 갱신, 없으면 생성한다 — 동일 Place ID로 중복 레코드가 생기지 않는다.
- 외부 필드(이름·주소·좌표·평점·평점 수)는 매 upsert마다 최신 응답 값으로 덮어쓰고 `external_data_refreshed_at`을 갱신한다.
- 큐레이션 필드(`suitable_for_*`)는 내부 편집 값이라 Google 갱신이 절대 건드리지 않는다.
- 이번 응답에 유형이 없거나 매핑되지 않으면(`FoodCategory.fromGooglePrimaryType()` 결과 없음) 기존 `food_category`를 그대로 유지한다 — 애매한 응답 하나 때문에 이미 알고 있는 분류를 지우지 않는다.

### 외부 지도 링크 URL 형식 (M1-5, 2026-09-20)

네이버·카카오 모두 우리가 갖고 있는 Google Place ID로 바로 열리는 링크를 지원하지 않는다. 두 서비스 다 이름 검색에 의존하는데, 동명 지점(체인점 등) 오매칭을 줄이려고 좌표를 지도 중심/핀 좌표로 함께 넘긴다.

- 카카오맵: `https://map.kakao.com/link/map/{이름},{위도},{경도}` — 카카오가 공식 지원하는 "핀 공유" 링크 형식으로, 좌표가 핀 위치를 직접 결정한다.
- 네이버지도: `https://map.naver.com/p/search/{이름}?c={경도},{위도},15,0,0,0,dh` — 검색 결과 자체는 이름 기준이지만 `c` 파라미터로 지도 중심을 좌표에 고정한다. 네이버는 카카오만큼 좌표 전용 핀 링크를 공식 제공하지 않아 차선책이다.
- 이름은 경로 세그먼트라 `URLEncoder`의 `+`(공백)를 `%20`으로 치환해서 인코딩한다.

### Google primaryType → FoodCategory 매핑 (M1-2, 2026-09-20 검증)

연남·한남·서촌·신사·논현 5곳에서 실제 `searchNearby` 호출로 얻은 약 99개 표본(반경 800m, `POPULARITY` 정렬)으로 검증했다. 코드의 `FoodCategory.fromGooglePrimaryType()`과 항상 동일하게 유지한다.

| FoodCategory | Google primaryType |
|---|---|
| KOREAN | `korean_restaurant`, `korean_barbecue_restaurant`, `chicken_restaurant`, `chicken_wings_restaurant`, `seafood_restaurant`, `noodle_shop` |
| JAPANESE | `japanese_restaurant`, `ramen_restaurant` |
| CHINESE | `chinese_restaurant` |
| WESTERN | `western_restaurant`, `italian_restaurant`, `european_restaurant`, `sandwich_shop`, `irish_pub`, `brunch_restaurant` |
| CAFE_DESSERT | `cafe`, `dessert_shop` |

미매핑 유형 처리 정책(로드맵 결정 항목 1 확정): **제외**. 임시 "기타" 카테고리로 묶지 않는다. 제외 대상은 표본 안에서 단일 cuisine을 신뢰 있게 특정할 수 없는 유형들이다 — 특정 하위 유형이 없는 generic `restaurant`(표본의 약 15%, 예: 프랜차이즈 치킨집·가정식 한식당), 식단/형식 라벨(`halal_restaurant`, `vegan_restaurant`, `buffet_restaurant`, `fine_dining_restaurant`), 5개 카테고리 밖 cuisine(`indian_restaurant`, `turkish_restaurant`), `asian_fusion_restaurant`, `meal_takeaway`.

표본에서 확인한 데이터 품질 지표:
- 검색 성공률: 5개 지역 모두 결과 반환 (5/5).
- 평점 노출률: 결과의 약 100%.
- 영업시간 노출률: 결과의 약 93%.
- 오매칭: `includedTypes: ["restaurant"]`로 제한했음에도 `hotel`이 반복적으로 섞여 나옴 (호텔 부속 식당이 호텔 장소로 색인된 경우) — 음식 카테고리가 아니므로 제외.

## 추천 기본 정책

- 검색 반경은 5km로 고정하고 직선 거리를 사용한다.
- 후보가 5개 미만이어도 반경을 자동으로 넓히지 않는다.
- 조회는 `RestaurantRepository.findWithinRadius()`(M1-4, 2026-09-20)가 담당한다. `ST_DWithin(location, point, radius)`로 GiST 인덱스(`restaurants_location_gist_idx`)를 태울 수 있는 형태를 쓰고, 거리순 정렬 결과를 반환한다. 반경은 호출자가 넘긴 값 그대로만 쓰고 메서드 내부에서 넓히지 않는다.
- 음식 카테고리는 한식·일식·중식·양식·카페/디저트 5종이다.
- 요청에는 음식 카테고리를 1개 이상, 동행 유형은 정확히 1개 포함할 수 있다.
- 동행 적합도는 제외 필터가 아니라 점수 가산 요소다.
- 목표 결과 개수는 5개이며, 후보가 그보다 적을 때의 동작은 추천 구현 작업에서 정한다.

## API와 운영

- 인증 없이 접근 가능한 엔드포인트는 `/api/v1/auth/signup`, `/api/v1/auth/login`이다.
- 그 외 모든 비즈니스 엔드포인트는 명시적으로 문서화되지 않는 한 인증(bearer token)이 필요하다.
- 예외: `GET /api/v1/restaurants/**`는 인증 없이 접근 가능하다. 식당 상세는 민감 정보가 아니고, 공유 링크는 비로그인 사용자도 열 수 있어야 한다. 식당 관련 쓰기/변경 작업은 여전히 인증이 필요하다.
- OpenAPI와 Swagger UI는 `local` Spring 프로필에서만 활성화된다.
- 스키마 변경은 기존 Member 마이그레이션 이력 다음부터 이어간다: Restaurant는 Flyway V5, V6를 사용한다.

## 구현 현황

- 구현 완료: Member 회원가입·로그인, JWT 필터, Restaurant 영속성 모델, PostGIS 위치 컬럼과 인덱스, 동행 적합도 플래그, CI 데이터베이스 서비스, 식당 상세 조회 API, Google Places 클라이언트(카테고리별 `includedTypes` 선처리 포함), Google primaryType 카테고리 매핑, Google Restaurant upsert·갱신 정책, PostGIS 5km 반경 후보 조회, 외부 지도 링크 API. → **M1 Restaurant 마일스톤 완료 (2026-09-20).**
- 다음 작업은 아래 순서로 진행한다.
  1. 문서 계약 정리: Notion API 상태, README 로컬 실행법, 초기 탐색 스팟 정책을 동기화한다.
  2. M2 ADR 확정: 추천 후보 조회 방식과 평점 보정·거리 정규화·동행 가산점 수치를 결정한다.
  3. M2-1: 추천 요청 DTO와 음식 카테고리·동행·위경도 입력 검증을 구현한다.
  4. M2-2: RecommendationSession과 후보·점수 구성 요소 스키마를 설계하고 마이그레이션한다.
  5. M2-3: ADR에 따른 점수 계산기와 경계값 테스트를 구현한다.
  6. M2-4~5: Top 5 추천 서비스와 추천 생성·세션 조회 API를 완성한다.
  7. M2.5 초기 탐색 스팟: 5개 지역 코드, 25개 식당·노출 순서 DB 시드, 탐색 조회 API를 구현한다.
  8. 이후 M3 Pick과 MVP E2E로 진행한다.

M2 착수 전 결정이 필요한 항목:

1. 평점 보정 공식·거리 정규화·동행 적합 가산점 수치 (ADR로 고정)
2. 추천 후보 조회 방식 — 매 요청 실시간 Google 호출 vs 우리 DB(`findWithinRadius`) 누적 조회 vs 하이브리드(DB 우선, 부족할 때만 Google 호출). 위 "Google Nearby Search 카테고리 선처리" 절 참고.

초기 탐색 스팟의 지역·개수·운영 방식은 확정됐지만 실제 25개 식당 목록과 각 지역 내 노출 순서는 DB 시드 작업 전에 별도로 확정한다.
