# Pick Eat 서비스 결정 사항

마지막 갱신일: 2026-09-21

## 제품 기준 문서

현재 Pick Eat 제품 기획만 제품 기준(source of truth)으로 사용한다.

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
- 실제 25개 식당과 노출 순서는 기획자가 전달할 ZIP 파일을 기준으로 확정한다. 파일을 받기 전까지 M2.5 구현은 보류한다.
- 가짜 식당을 운영 DB용 Flyway 시드로 만들지 않는다. 개발 중 임시 데이터가 필요하면 테스트 fixture 또는 API mock에만 둔다.

이 25곳은 **탐색 스팟 전용 데이터**다. M2 위치 기반 추천의 후보 정책을 대신하지 않으며, 추천에서는 다른 Restaurant와 동일하게 5km·음식 카테고리 조건을 통과할 때만 후보가 될 수 있다.

### Google Nearby Search 카테고리 선처리 (2026-09-20)

`GooglePlacesClient.findNearbyRestaurants()`는 이제 `includedTypes`를 호출자가 넘긴다. 추천 요청에 카테고리가 있으면 `FoodCategory.toGooglePrimaryTypes()`(M1-2 매핑표의 역방향)로 구체적인 Google 하위 타입 목록을 만들어 넘기고, 카테고리가 없으면 `Set.of("restaurant")`로 광범위하게 요청한다.

- Google Nearby Search는 한 번 호출에 최대 20개까지만 반환하고, 이건 "반경 안 전체를 찾은 뒤 자르는" 게 아니라 Google이 자기 랭킹(`POPULARITY`/`DISTANCE`) 기준으로 골라주는 상위 20개다.
- `includedTypes`를 카테고리에 맞게 좁히면 이 20개 슬롯이 hotel·halal_restaurant처럼 어차피 제외할 타입에 낭비되지 않고 관련 있는 후보로 채워진다.
- 추천 후보는 M2 ADR에 따라 매 요청 Google Nearby Search를 호출하고 응답을 DB에 upsert한 뒤 5km 반경 후보를 조회한다. 전환 조건은 아래 "M2 ADR 확정 결과"를 따른다.

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
- 목표 결과 개수는 5개다. 조건을 만족하는 후보가 5개보다 적으면 반경을 넓히지 않고 조회된 후보만 반환한다.

## API와 운영

- 인증 없이 접근 가능한 엔드포인트는 `/api/v1/auth/signup`, `/api/v1/auth/login`이다.
- 그 외 모든 비즈니스 엔드포인트는 명시적으로 문서화되지 않는 한 인증(bearer token)이 필요하다.
- 예외: `GET /api/v1/restaurants/**`는 인증 없이 접근 가능하다. 식당 상세는 민감 정보가 아니고, 공유 링크는 비로그인 사용자도 열 수 있어야 한다. 식당 관련 쓰기/변경 작업은 여전히 인증이 필요하다.
- OpenAPI와 Swagger UI는 `local` Spring 프로필에서만 활성화된다.
- 스키마 변경은 기존 Member 마이그레이션 이력 다음부터 이어간다: Restaurant는 Flyway V5·V6, Recommendation은 V7, Pick은 V8을 사용한다. 초기 탐색 스팟은 V9 이후를 사용한다.

## 배포 결정

- 운영 애플리케이션 서버는 AWS EC2를 사용한다.
- CI와 CD는 GitHub Actions를 사용한다. CD는 `main` CI 성공 후 실행 JAR를 EC2에 전송한다. `DEPLOY_ENABLED=true` 전에는 실행하지 않는다.
- EC2에서는 Java 21 실행 JAR를 systemd로 관리한다. 릴리스별 디렉터리와 `current` 심볼릭 링크를 사용하고, `/actuator/health` 실패 시 직전 릴리스로 자동 롤백한다.
- 운영 공개 전 HTTPS 종료 지점(Nginx 또는 ALB)과 도메인을 확정한다. 애플리케이션 8080 포트는 외부에 직접 공개하지 않는다.
- 운영 PostgreSQL/PostGIS는 RDS 또는 별도 호스트 중 인프라 생성 시 확정한다. 애플리케이션은 `DB_URL` 등 환경변수만 사용한다.
- 운영 비밀 값(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `GOOGLE_PLACES_API_KEY`)은 저장소에 커밋하지 않고 GitHub Actions Secrets 또는 AWS의 비밀 저장소에서 주입한다.

## 구현 현황

- 구현 완료: Member 회원가입·로그인, JWT 필터, Restaurant 영속성 모델, PostGIS 위치 컬럼과 인덱스, 동행 적합도 플래그, CI 데이터베이스 서비스, 식당 상세 조회 API, Google Places 클라이언트(카테고리별 `includedTypes` 선처리 포함), Google primaryType 카테고리 매핑, Google Restaurant upsert·갱신 정책, PostGIS 5km 반경 후보 조회, 외부 지도 링크 API. → **M1 Restaurant 마일스톤 완료 (2026-09-20).**
- M2 작업은 아래 순서로 진행한다. 완료된 항목은 체크하고, 항목을 마칠 때마다 이 목록을 갱신한다.
  1. [x] 문서 계약 정리: Notion API 상태, README 로컬 실행법, 초기 탐색 스팟 정책을 동기화한다. (PR #24)
  2. [x] M2 ADR 확정: 추천 후보 조회 방식과 평점 보정·거리 정규화·동행 가산점 수치를 결정한다. → 아래 "M2 ADR 확정 결과" 참고.
  3. [x] M2-1: 추천 요청 DTO와 음식 카테고리·동행·위경도 입력 검증을 구현한다. (PR #25)
  4. [x] M2-2: RecommendationSession과 후보·점수 구성 요소 스키마를 설계하고 마이그레이션한다. (PR #27)
  5. [x] M2-3: ADR에 따른 점수 계산기와 경계값 테스트를 구현한다. (PR #26)
  6. [x] M2-4~5: Top 5 추천 서비스와 추천 생성·세션 조회 API를 완성한다. (PR #28)
  7. [ ] **M2.5: 초기 탐색 스팟.** 5개 지역 코드, 25개 식당·노출 순서 DB 시드, 탐색 조회 API를 구현한다. 기획 ZIP 수신 전까지 보류한다.
  8. [x] **M3: Pick.** 최종 선택 기록·상태 변경·목록·지도 조회를 구현한다.
  9. [ ] **배포/CD.** GitHub Actions CD·systemd·헬스체크·자동 롤백 구성은 완료. EC2·운영 DB 생성, Secrets 등록, HTTPS 연결과 최초 실배포가 남아 있다.
  10. [ ] 프론트엔드 연동 MVP E2E를 검증한다.

### M2 ADR 확정 결과 (2026-09-21)

- **점수 계산 공식:** 원시 Google 평점(보정 없음) + 거리 선형 정규화(`1 - distance/5000`) + 동행 적합 고정 가산점. 가중치는 코드 상수(`w1=0.6, w2=0.4, bonus=0.1`)로 시작 — 실사용 리뷰/트래픽 데이터가 쌓여 왜곡이 실측되면 베이지안 보정으로 승격.
- **추천 후보 조회 방식:** 매 요청 실시간 Google Nearby Search 호출 + 결과를 `upsertFromGoogle`로 DB에 반영. API 비용이 부담되거나 DB 지역 커버리지가 충분해지면 "DB 우선, 부족할 때만 Google" 하이브리드로 전환.

### M2-4~5 구현 결과 (2026-09-21)

- `POST /api/v1/recommendations`(추천 생성) / `GET /api/v1/recommendations/{sessionId}`(세션 조회) 추가. 둘 다 인증 필요, 세션 조회는 요청자가 세션 소유자가 아니면 404.
- 추천 후보는 Google Nearby Search 응답을 upsert한 뒤 `RestaurantRepository.findWithinRadius()`로 다시 조회해 거리를 얻는다 — CURATED 데이터가 생기는 M2.5 이후에도 같은 조회로 자동 포함된다.
- 후보가 0개면 빈 `items`, 1~4개면 조회된 개수만, 5개를 초과하면 점수순 상위 5개만 반환하는 경계 테스트를 고정했다.
- 추천 생성·세션 조회의 JWT 인증, HTTP 상태, 요청 검증 오류(`GLOBAL_001`), JSON 응답 필드를 MockMvc 계약 테스트로 검증한다.
- 전체 테스트 134개가 통과한다.

초기 탐색 스팟의 지역·개수·운영 방식은 확정됐지만 실제 25개 식당 목록과 각 지역 내 노출 순서는 DB 시드 작업 전에 별도로 확정한다.

### M2.5 우선도와 보류 기준

- 홈 탐색 경험에 필요하므로 제품 우선도는 높다.
- 다만 실제 식당 목록과 노출 순서가 기획 ZIP에 의존하므로 현재 구현 우선도는 보류 상태다.
- ZIP 수신 후 데이터 형식 검수 → 지역 코드·스키마 확정 → V9 이후 실제 데이터 시드 → 탐색 조회 API·테스트 순서로 재개한다.

### M3 Pick 구현 결과 (2026-09-21)

- 추천 세션당 최종 Pick은 하나만 허용한다. 같은 식당은 서로 다른 추천 세션에서 다시 Pick할 수 있다.
- Pick 대상은 요청한 회원이 소유한 추천 세션에 실제 후보로 저장된 식당이어야 한다. 다른 회원의 세션·Pick은 404로 처리해 존재 여부를 노출하지 않는다.
- 상태는 `SELECTED`, `REVIEWED`, `CANCELED` 세 가지다. `SELECTED`에서 `REVIEWED` 또는 `CANCELED`로만 전환하며, 같은 상태 요청은 멱등 처리하고 종료 상태는 되돌리지 않는다.
- `REVIEWED` 전환 시 `visited_at`을 기록한다. `CANCELED` Pick은 이력 목록에는 남기되 지도 조회에서는 제외한다.
- 목록은 `selected_at DESC, id DESC`로 안정 정렬하고 페이지 조회한다.
- API는 `POST /api/v1/picks`, `PATCH /api/v1/picks/{pickId}`, `GET /api/v1/me/picks`, `GET /api/v1/me/picks/map` 네 개이며 모두 JWT 인증이 필요하다.
- Flyway V8에 `picks` 테이블, 추천 세션 유일 제약, 회원별 최신순·상태·식당 인덱스를 추가했다.
- 엔티티 상태 전이, 서비스 권한·중복·후보 검증, DB 제약, JWT 및 JSON 계약을 포함해 전체 테스트 134개가 통과한다.
