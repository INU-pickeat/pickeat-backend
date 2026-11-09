# Pick Eat 서비스 결정 사항

지금 유효한 제품·기술 결정만 둔다. 결정이 바뀌면 이 문서를 덮어쓰고, 바뀐 경위는 [history.md](history.md)에 남긴다. 진행 현황은 [progress.md](progress.md)에 있다.

## 제품 기준 문서

현재 Pick Eat 제품 기획만 제품 기준(source of truth)으로 사용한다.

## 식당 데이터 전략

- 추천 후보는 Google Places 주변 검색(nearby search)에서 가져온다.
- 서비스는 안정적인 내부 ID, Pick, 후기, 외부 데이터 갱신에 필요한 최소한의 식당 스냅샷만 저장한다.
- `GOOGLE`과 `CURATED`는 `data_provider`로 구분해서 기록한다.
- 큐레이션 데이터는 초기 탐색 경험 전용이다: 신사·혜화·서촌·한남·종로 각 5곳씩 총 25곳.
- Google 평점과 향후 Pick Eat 자체 평점은 의미와 갱신 정책이 달라서 분리해서 관리한다.

### 초기 인기맛집 탐색 스팟 고정 정책 (2026-09-21, 2026-09-23 목록 확정)

초기에는 실제 Picker와 Pick·좋아요·후기 데이터가 없고 실시간 인기 집계 기능도 구현하지 않는다. 따라서 탐색 스팟과 노출 식당은 자동 순위가 아니라 운영자가 선정한 고정 큐레이션으로 제공한다.

- 최종 고정 지역: `신사(SINSA)`, `혜화(HYEHWA)`, `서촌(SEOCHON)`, `한남(HANNAM)`, `종로(JONGNO)`.
- 지역별 식당은 정확히 5곳, 총 25곳으로 시작한다.
- 지역 목록은 초기 애플리케이션 계약에 고정하되 안정적인 지역 코드로 표현해서 이후 지역 추가가 가능하게 한다.
- 식당과 노출 순서는 소스 코드의 임시 객체가 아니라 Flyway 마이그레이션으로 DB에 직접 시드한다. 식당은 `data_provider=CURATED`로 구분한다.
- 동일 마이그레이션을 다시 적용해도 중복 식당이 생기지 않도록 안정적인 식별·중복 방지 기준을 함께 둔다.
- 초기 탐색 결과를 사용자 행동 기반의 "실시간 인기 순위"라고 표현하지 않는다. 현재 의미는 **운영자 선정 인기 후보**다.
- Pick·좋아요·후기 데이터와 집계 기능이 충분해지면 고정 목록을 동적 랭킹으로 교체한다. 그 전까지 자동 갱신·인기 점수·집계 배치는 구현하지 않는다.
- 실제 25개 식당과 지역 내 노출 순서는 2026-09-23 전달 목록으로 확정했다. 원본은 `src/main/resources/curated/discovery-spots.csv`로 보존한다.
- 2026-10-08 정정(V25): V14 시드의 식당·순서가 사진과 어긋나 있어 전달 목록대로 바로잡았다. 혜화(학림다방 3위, 정돈·아란치아 추가), 한남(쥬에 1위·타크 2위, 바다식당·바바라스키친 추가), 종로(대장장이화덕피자 추가). 빠진 5곳은 일반 Google 식당으로 돌리고 탐색 사진을 지웠다. DB 순서와 CSV가 같은지는 `RestaurantSchemaIntegrationTest`가 확인한다.
- 2026-10-10 정정(V28): 종로 4위 대장장이화덕피자의 소개·영업시간·전화번호를 최신 전달 내용으로 고치고, 종로 4·5위의 서로 뒤바뀐 대표 사진을 바로잡았다.
- 한남에서 음식 종류가 생략된 항목은 기획 확인에 따라 `쥬에=CHINESE`, `오만지아=WESTERN`, `이태원우육미옌=CHINESE`, `마일스톤커피 한남=CAFE_DESSERT`로 확정했다. `타크`는 `OTHER`다.
- 대표 사진 25장은 JPG로 수령해 `{region}_{01..05}_main.jpg` 규칙으로 정규화했다. EXIF 메타데이터는 제거하고 `/images/discovery/{파일명}`에서 공개 제공하며, V15에서 각 Restaurant의 이미지 URL을 연결한다.
- 가짜 식당을 운영 DB용 Flyway 시드로 만들지 않는다. 개발 중 임시 데이터가 필요하면 테스트 fixture 또는 API mock에만 둔다.

이 25곳은 **탐색 스팟 전용 데이터**다. M2 위치 기반 추천의 후보 정책을 대신하지 않으며, 추천에서는 다른 Restaurant와 동일하게 검색 반경(수도권·부산 1km, 그 외 5km)·음식 카테고리 조건을 통과할 때만 후보가 될 수 있다.

### Google Nearby Search 카테고리 선처리 (2026-09-20)

`GooglePlacesClient.findNearbyRestaurants()`는 이제 `includedPrimaryTypes`를 호출자가 넘긴다. 추천 요청에 카테고리가 있으면 `FoodCategory.toGooglePrimaryTypes()`(M1-2 매핑표의 역방향)로 구체적인 Google 하위 타입 목록을 만들어 넘기고, 카테고리가 없으면 `Set.of("restaurant")`로 광범위하게 요청한다.

- Google Nearby Search는 한 번 호출에 최대 20개까지만 반환하고, 이건 "반경 안 전체를 찾은 뒤 자르는" 게 아니라 Google이 자기 랭킹(`POPULARITY`/`DISTANCE`) 기준으로 골라주는 상위 20개다.
- `includedPrimaryTypes`를 카테고리에 맞게 좁히면 이 20개 슬롯이 hotel·halal_restaurant처럼 어차피 제외할 타입에 낭비되지 않고 관련 있는 후보로 채워진다.
- 추천 후보는 DB에서 검색 반경 안의 유효 후보를 먼저 조회하고, 합계가 10개 미만이거나 요청한 카테고리 중 5개 미만인 카테고리가 있을 때만 부족한 카테고리를 Google Nearby Search로 보충해 upsert한 뒤 다시 조회한다(2026-09-22 개편, 2026-10-04 카테고리별 기준 추가). 초기 계약이던 "매 요청 Google 호출"은 폐기했다.

### Google upsert 정책 (M1-3, 2026-09-20)

- `google_place_id` 기준으로 조회 후 있으면 갱신, 없으면 생성한다 — 동일 Place ID로 중복 레코드가 생기지 않는다.
- 외부 필드(이름·주소·좌표·평점·평점 수)는 매 upsert마다 최신 응답 값으로 덮어쓰고 `external_data_refreshed_at`을 갱신한다.
- 큐레이션 필드(`suitable_for_*`)는 내부 편집 값이라 Google 갱신이 절대 건드리지 않는다.
- 이번 응답에 유형이 없으면 기존 `food_category`를 그대로 유지한다 — 애매한 응답 하나 때문에 이미 알고 있는 분류를 지우지 않는다.
- 기존 6개 명시 카테고리에 매핑되지 않더라도 Google Food and Drink 허용 목록에 포함된 유형은 `OTHER`로 저장한다. 알려지지 않은 타입과 비음식 장소는 `OTHER`로 자동 편입하지 않고 제외한다.

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
| JAPANESE | `japanese_restaurant`, `ramen_restaurant`, `japanese_izakaya_restaurant` |
| CHINESE | `chinese_restaurant` |
| WESTERN | `western_restaurant`, `italian_restaurant`, `european_restaurant`, `sandwich_shop`, `brunch_restaurant` |
| CAFE_DESSERT | `cafe`, `dessert_shop` |
| PUB_BAR | `bar`, `pub`, `wine_bar`, `cocktail_bar`, `sports_club`, `gastropub`, `brewpub`, `brewery`, `lounge_bar`, `hookah_bar`, `irish_pub`, `beer_garden`, `bar_and_grill` |
| OTHER | 위 6개 카테고리에 속하지 않는 Google Food and Drink 허용 타입 |

미매핑 유형 정책은 2026-09-22에 변경했다. 기존 6개 카테고리 밖의 멕시칸·태국·인도·베트남·아시안 등 Google Food and Drink 타입은 `OTHER`로 분류한다. `cat_cafe`, `dog_cafe`, `hotel` 등 명시 제외 타입과 Food and Drink 허용 목록에 없는 알려지지 않은 타입은 제외한다. Google 타입 목록이 바뀌어도 임의의 신규 타입이 자동으로 `OTHER`에 들어가지 않도록 허용 목록을 코드와 문서에서 함께 관리한다.

표본에서 확인한 데이터 품질 지표:
- 검색 성공률: 5개 지역 모두 결과 반환 (5/5).
- 평점 노출률: 결과의 약 100%.
- 영업시간 노출률: 결과의 약 93%.
- 오매칭: `includedTypes: ["restaurant"]` 실측 당시 `hotel`이 반복적으로 섞여 나옴 (호텔 부속 식당이 호텔 장소로 색인된 경우) — 음식 카테고리가 아니므로 제외. 현재 구현은 주 유형만 거르는 `includedPrimaryTypes`를 사용한다.

### 추천 구조 개편 결정: 식당 DB 제거 + 실시간 검색 + 랜덤 추천 (2026-11-09, 구현 전)

결정만 했고 아직 구현하지 않았다. 구현하면 [history.md](history.md) "Google 보충", "추천 기본 정책"의 DB 조회·점수 정렬 내용을 이 결정으로 바꾼다.

**배경**
- Google Maps Platform 약관상 저장할 수 있는 건 `place_id`(영구)와 좌표(30일)뿐이다. 지금은 이름·주소·평점·가격·영업시간·전화번호·적합도·주차를 `restaurants`에 기한 없이 저장한다(DB 우선 조회 + 부족할 때 Google 보충 후 upsert). 약관 위반이고 API 키 정지 위험이 있다.
- 같은 위치에서 추천하면 점수 내림차순 상위 5개라 늘 같은 결과가 나온다.

**결정**
1. **식당 DB 제거.** 식당 정보는 매번 Google에서 실시간 조회한다. `restaurants` 행은 Pick·후기가 참조하므로 `id`와 `google_place_id`만 남기고 Google 필드는 지운다. 좌표는 30일 이내 캐시만 허용. 운영자가 넣은 CURATED(탐색 스팟 25곳)는 우리 데이터라 유지한다.
2. **후보 검색은 동행별 "장소 유형 묶음 + 검색어"로 한다.**
   - 장소 유형 묶음: Nearby Search 1번(`includedPrimaryTypes` 최대 50개). 영업 중·평점 필터가 없어 받은 뒤 거른다.
   - 검색어: 동행별 검색어 목록에서 매 추천마다 1~2개를 랜덤으로 골라 Text Search. `openNow`, `minRating`, `priceLevels`를 요청에 넣어 Google에서 먼저 거른다. 범위는 사각형만 가능해 반경을 감싸는 사각형으로 받는다.
   - 추천 1회당 Google 호출 2~3번.
3. **필터.** 평점 없는 식당은 제외한다. 반경(원) 밖, 가격 범위(Google은 단계라 받은 뒤 원 단위로 다시 확인), 주차(받은 뒤), 데이트 프랜차이즈(받은 뒤, `franchise-brands.txt`)를 거른다.
4. **랜덤 추천.** 필터를 통과한 후보에서 최대 20개를 랜덤 순서로 골라 세션에 `place_id`와 순서만 저장한다. 앞 5개를 보여주고 나머지는 제외 시 대체 후보로 쓴다(기존 10개 → 20개). 같은 유형 개수 상한은 두지 않는다. 쏠리면 다시 추천으로 해결한다. 세션의 후보는 모두 최소 기준을 통과한 상태다. 단, 영업 중 여부는 추천 생성 시각 기준이다.
5. **동행별 목록.**
   - 데이트 장소 유형(21): 이탈리안·프렌치·스페인·지중해·유럽식·양식·스테이크·비스트로·피자·브런치·와인바·칵테일바·라운지바·가스트로펍·이자카야·스시·야키니쿠·야키토리·멕시칸·태국·광둥식/딤섬·퓨전/아시안 퓨전. 검색어: 파인다이닝, 오마카세, 한식 다이닝, 코스요리, 뷰 맛집. 뺀 것: 고깃집·해산물·일식 전반·베트남·인도·디저트·펍·샤브샤브.
   - 데이트 예외: 뺀 유형이라도 아주 유명한 곳(예: 금돼지식당)은 포함한다. 기준(예: 평점 4.3 이상 + 리뷰 1,000개 이상)은 실제 값을 보고 확정한다.
   - 혼밥 장소 유형: 라멘·스시·일본식 카레·돈카츠·햄버거·국수·샌드위치. 검색어: 국밥, 덮밥.
   - 나머지 동행(가족·아이·단체·반려견)은 미정.
6. 프랜차이즈 목록에 파파존스를 추가한다.

**실측 (2026-11-09, 혜화·강남·홍대 반경 1km, 27번 호출)**
- 장소 유형 묶음만 쓰면 한 유형으로 쏠린다(홍대 혼밥 라멘 13/20, 혜화 데이트 이탈리안 16/20). 국밥·덮밥은 유형이 없어 안 나온다.
- 검색어를 더하면 후보가 40~60개, 메뉴 5~8종으로 늘었다. "국밥"·"덮밥" 결과는 거의 전부 맞았고, "치즈"는 치즈버거·피자가 나와 데이트 검색어에서 뺐다.
- "혼밥"·"데이트 맛집" 한 단어 검색은 분위기는 맞지만 가구점·단체 같은 비음식점과 이름에 키워드를 넣은 노출용 가게가 섞였다.
- 결과 대부분이 평점 3.5 이상이라 최저 평점 필터는 평점 없는 곳을 빼는 역할이 크다.

**비용 (Places API (New) 2026-11 요금표, 월 무료 한도 반영)**
- 단가(1,000건): Nearby·Text Search Enterprise+Atmosphere $40, Place Details Enterprise+Atmosphere $25 / Enterprise $20 / Pro $17, Place Photo $7.
- 가정(사용자 1명/월): 추천 20회, 상세 15회, 목록에 식당 100건 노출. 최적화(주차·아이·반려견 필드 제외, 세션 목록 재조회 안 함) 후 월 사용자 30명 $0, 100명 약 $180, 500명 약 $1,500, 2,000명 약 $6,500. 검색어 호출 추가로 추천 부분은 이보다 늘어난다.
- 가장 큰 비용은 목록 화면(Pick·캘린더·지도·피드)의 식당 이름 조회다. 이름은 저장할 수 없어서 줄이기 어렵다.
- 사용자 테스트 규모에서는 무료 한도 안이다. 정식 출시 후 사용자가 수백 명이 되면 다시 판단한다. 대안은 [progress.md](progress.md) 44번(공공데이터 원본)이다.

**구현 순서(예정)**: ① 실시간 조회 서비스 + 식당 상세 ② 목록 화면(이름 조회) ③ 추천(위 결정) ④ Google 컬럼 삭제 마이그레이션. ④는 앞 단계가 운영에 배포된 뒤 한다.

**미정**: 데이트 유명 맛집 예외 기준, 나머지 동행 목록, 기존 7개 음식 카테고리와 메뉴명 기반 추천의 관계, 세션 유효 시간, 실제 트래픽 기준 비용 재계산.

## 추천 기본 정책

- 검색 반경은 사용자가 고를 수 있다. 수도권(서울·인천·경기)·부산(위경도 사각형 근사)은 500m·1km·2km, 그 외 지역은 1km·3km·5km이고, 고르지 않으면 수도권·부산 1km, 그 외 5km다(2026-10-13). 직선 거리를 쓰며 거리 점수 정규화도 같은 반경을 쓴다.
- 후보가 5개 미만이어도 반경을 자동으로 넓히지 않는다.
- 조회는 `RestaurantRepository.findWithinRadius()`(M1-4, 2026-09-20)가 담당한다. `ST_DWithin(location, point, radius)`로 GiST 인덱스(`restaurants_location_gist_idx`)를 태울 수 있는 형태를 쓰고, 거리순 정렬 결과를 반환한다. 반경은 호출자가 넘긴 값 그대로만 쓰고 메서드 내부에서 넓히지 않는다.
- 음식 카테고리는 한식·일식·중식·양식·카페/디저트·펍/와인/술집·기타 7종이다.
- 요청에는 음식 카테고리를 1개 이상, 동행 유형은 정확히 1개 포함할 수 있다.
- 동행 유형은 데이트·가족·아이·혼밥·단체·반려견 6종이다. 기존 `FRIENDS`는 별도 유형으로 유지하지 않고 `GROUP`에 포함한다.
- 사용자가 가격 범위를 지정하면 식당의 Google `priceRange`와 일부라도 겹치는 후보만 포함한다. `priceRange` 필드를 생략하면 가격 무관이며 가격 정보가 없는 식당도 포함한다. 명시적 `null`도 호환상 가격 무관으로 처리하되 API 예제는 필드 생략을 표준으로 사용한다.
- 동행 적합도는 제외 필터가 아니라 점수 가산 요소다.
- 목표 결과 개수는 5개다. 조건을 만족하는 후보가 5개보다 적으면 반경을 넓히지 않고 조회된 후보만 반환한다.
- 추천 세션의 내부·API 식별자는 `Long`을 유지한다. 외부 공유가 필요해지면 별도 UUID 공개 식별자를 추가한다(2026-09-22).

## API와 운영

- 인증 없이 접근 가능한 엔드포인트는 `/api/v1/auth/email-verifications`, `/api/v1/auth/email-verifications/confirm`, `/api/v1/auth/signup`, `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/api/v1/auth/logout`, `GET /api/v1/discovery-spots`, `GET /images/discovery/**`이다.
- 그 외 모든 비즈니스 엔드포인트는 명시적으로 문서화되지 않는 한 인증(bearer token)이 필요하다.
- 예외: `GET /api/v1/restaurants/**`는 인증 없이 접근 가능하다. 식당 상세는 민감 정보가 아니고, 공유 링크는 비로그인 사용자도 열 수 있어야 한다. 식당별 후기 요약(`GET /api/v1/restaurants/{id}/review-summary`)도 이 경로 아래라 인증 없이 조회한다. 식당 관련 쓰기/변경 작업은 여전히 인증이 필요하다.
- OpenAPI와 Swagger UI는 `local` 프로필에서 인증 없이 켜진다. 운영은 프론트 요청으로 2026-11-09부터 `SWAGGER_ENABLED=true`일 때 문서 전용 HTTP Basic 계정(`DOCS_USERNAME`/`DOCS_PASSWORD`)으로 연다.
- 스키마 변경은 기존 Member 마이그레이션 이력 다음부터 이어간다: Restaurant는 Flyway V5·V6, Recommendation은 V7, Pick은 V8을 사용한다. 2026-09-22 계약 변경은 V9부터 사용하고 초기 탐색 스팟은 V13~V15, Refresh Token은 V18, Review·이미지·좋아요는 V19를 사용한다.

## 배포 결정

- 운영 애플리케이션 서버는 AWS EC2를 사용한다.
- CI와 CD는 GitHub Actions를 사용한다. CD는 `main` CI 성공 후 실행 JAR를 EC2에 전송한다. `DEPLOY_ENABLED=true` 전에는 실행하지 않는다.
- EC2에서는 Java 21 실행 JAR를 systemd로 관리한다. 릴리스별 디렉터리와 `current` 심볼릭 링크를 사용하고, `/actuator/health` 실패 시 직전 릴리스로 자동 롤백한다.
- HTTPS 종료 지점은 Nginx, 도메인은 `api.pickeat.kr`로 확정했고 2026-10-04 운영에 적용했다. 애플리케이션 8080 포트는 외부에 직접 공개하지 않는다.
- 운영 인스턴스는 AWS EC2 프리티어 t3.micro(1GB RAM) 한 대로 시작한다(2026-09-28 확정). JVM 힙은 `-Xmx384m`으로 제한하고 2GB swap을 둔다.
- 운영 PostgreSQL/PostGIS는 같은 인스턴스의 Docker 컨테이너(`postgis/postgis:16-3.4`, CI와 같은 이미지)로 운영한다(2026-09-28 확정). `127.0.0.1`에만 바인딩하고 매일 `pg_dump` 백업과 EBS 스냅샷으로 보존한다. 메모리가 부족해지면 DB만 RDS로 옮기며, 애플리케이션은 `DB_URL` 등 환경변수만 사용한다.
- API 도메인은 `api.pickeat.kr`이며 Nginx + Let's Encrypt로 HTTPS를 종료한다. `/actuator/**`는 외부에 노출하지 않는다.
- GitHub Actions의 EC2 배포는 SSH(22번)로 한다. GitHub-hosted runner의 IP가 고정되지 않아 22번 인바운드를 `0.0.0.0/0`으로 열고, 키 인증 전용(`PasswordAuthentication no`)과 fail2ban으로 보완한다(2026-10-01). 여유가 생기면 OIDC + SSM 배포로 바꿔 22번을 닫는다.
- 후기 이미지는 S3 presigned URL로 클라이언트가 직접 올리고 서버는 URL만 저장한다(2026-10-04). 버킷이 설정되지 않은 환경에서도 애플리케이션은 기동하며 업로드 URL 발급만 `REVIEW_006`으로 거부한다. 자격 증명은 EC2 인스턴스 역할에서 읽는다.
- 운영 비밀 값(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `GOOGLE_PLACES_API_KEY`)은 저장소에 커밋하지 않고 GitHub Actions Secrets 또는 AWS의 비밀 저장소에서 주입한다.
