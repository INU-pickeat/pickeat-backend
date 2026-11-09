# 식당

코드: `src/main/java/com/pickeat/pickeatbackend/domain/restaurant/`. 이 도메인의 현재 규칙과 결정 기록이다. 진행 현황은 [progress.md](../progress.md), API 명세 원본은 Notion BE > API SPECIFICATION이다.

## 현재 규칙

- 추천 후보는 Google Places 주변 검색(nearby search)에서 가져온다.
- 서비스는 안정적인 내부 ID, Pick, 후기, 외부 데이터 갱신에 필요한 최소한의 식당 스냅샷만 저장한다.
- `GOOGLE`과 `CURATED`는 `data_provider`로 구분해서 기록한다.
- 큐레이션 데이터는 초기 탐색 경험 전용이다: 신사·혜화·서촌·한남·종로 각 5곳씩 총 25곳.
- Google 평점과 향후 Pick Eat 자체 평점은 의미와 갱신 정책이 달라서 분리해서 관리한다.
- 예외: `GET /api/v1/restaurants/**`는 인증 없이 접근 가능하다. 식당 상세는 민감 정보가 아니고, 공유 링크는 비로그인 사용자도 열 수 있어야 한다. 식당별 후기 요약(`GET /api/v1/restaurants/{id}/review-summary`)도 이 경로 아래라 인증 없이 조회한다. 식당 관련 쓰기/변경 작업은 여전히 인증이 필요하다.
- Google 데이터·사진을 응답에 담으면 출처를 함께 내려준다. 식당 상세·추천 항목의 `dataAttribution`("Google Maps"/null), 사진 촬영자는 `GET /api/v1/restaurants/{id}/photo/attributions`(2026-11-09, #80).

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

## 결정 기록 (최신순)

구현 당시의 판단과 결과다. 현재 규칙과 다르면 현재 규칙이 우선이다.

### Google 식당 사진 연동 (2026-10-08)

- 문제: 사진이 있는 식당은 탐색 스팟 25곳뿐이었다. 추천에 나오는 Google 출처 식당은 사진을 받아오지 않아 프론트에서 대부분 이미지가 비어 있었다.
- `GET /api/v1/restaurants/{id}/photo`를 추가했다. Google Places에서 대표 사진의 임시 주소를 받아 302로 넘겨준다. API 키를 프론트에 노출할 수 없어서 백엔드를 거친다.
- 자체 이미지(`representative_image_url`)가 없고 Google 장소 ID가 있는 식당은 식당 상세·추천 결과의 `representativeImageUrl`로 이 API 주소를 내려준다. Google에 사진이 실제로 있는지는 응답 시점에 알 수 없어서, 사진이 없는 식당은 이 주소가 404 `RESTAURANT_002`를 응답한다. 프론트는 이미지 로드 실패 시 기본 이미지를 보여줘야 한다.
- Google 약관상 사진 이름(`photos[].name`)은 저장할 수 없고 만료된다. 그래서 DB에 저장하지 않고, 요청마다 Place Details(`photos` 필드만)로 이름을 새로 받은 뒤 Place Photo로 주소를 받는다. 이미 DB에 있던 식당도 별도 작업 없이 사진이 나온다.
- 비용: 사진 조회 1회마다 Place Details 1회와 Place Photo 1회를 호출한다. 호출을 줄이려고 받은 주소(사진 없음 포함)를 서버 메모리에 30분, 브라우저에 30분(`Cache-Control`) 기억한다. 사진 크기는 가로 최대 800px이다.
- 2026-10-08 추가: 식당 상세에서는 사진을 3장까지 보여준다. 사진 API가 `index`(0~2)를 받고, 식당 상세 응답에 `imageUrls`를 추가했다. 자체 이미지가 있으면 그것이 첫 번째이고 나머지를 Google 사진으로 채운다. 추천 목록은 비용 때문에 1장을 유지한다. 사진 한 장마다 Place Photo 요금이 붙으므로 상세를 열 때마다 최대 2건이 더 든다.
- 한계: 캐시는 인스턴스 메모리라 재시작하면 비워진다. 인증 없는 API라 식당 ID를 바꿔 가며 호출하면 Google 요금이 늘 수 있다. Google Cloud 콘솔에서 Places API 일일 할당량 상한을 걸어 두어야 한다.

### 이름 키워드로 음식 카테고리 보정 (2026-10-08)

- 문제: Google이 주는 유형이 실제 음식과 다른 식당이 있다. 운영에서 "더샤브칼국수", "송도장어촌", "호우양꼬치"가 일식으로 추천됐다. 더샤브칼국수는 Google 유형이 `japanese_restaurant`뿐이라 유형 매핑을 바꿔서는 고칠 수 없다.
- 식당 이름에 키워드가 있으면 Google 유형보다 그 카테고리를 우선한다(`FoodCategoryNameOverride`). 키워드는 `src/main/resources/curated/category-name-keywords.txt`에서 관리한다. 칼국수·게장·찜닭·장어 등은 한식, 양꼬치·마라탕 등은 중식이다. "샤브"는 키워드로 쓰지 않는다.
- Google에서 식당을 받아 저장할 때 적용한다. 이미 저장된 Google 출처 식당은 V22 마이그레이션으로 한 번 고쳤다. 탐색 스팟(CURATED)은 대상이 아니다.
- 한계: 이름에 키워드가 들어가기만 하면 걸린다. "장어덮밥"을 파는 일식집도 한식이 된다. 키워드 파일을 고쳐도 이미 저장된 식당은 Google에서 다시 받아올 때까지 그대로라, 기존 식당까지 바꾸려면 마이그레이션을 추가해야 한다.

### 카테고리 키워드 확장과 보조 카테고리 (2026-10-08)

- 이름 키워드 보정을 전체 카테고리(한식·중식·일식·양식·카페·주점·기타)로 넓혔다. Google이 `restaurant`로만 주는 식당도 이름에 키워드가 있으면 분류된다.
- 팀 결정: 이자카야는 일식과 주점 둘 다, 치킨은 한식과 주점 둘 다. 돈까스는 한식, 돈카츠는 일식. 만두는 한식, 딤섬은 중식. 횟집·물회·막회는 한식, 스시·초밥은 일식.
- 두 카테고리에 걸치는 식당을 위해 `restaurants.secondary_food_category`를 추가했다(V23). 추천 후보를 고를 때 요청 카테고리가 첫 번째나 보조 카테고리 중 하나와 맞으면 포함한다. 응답의 `foodCategory`는 항상 첫 번째 카테고리다.
- 여러 키워드가 걸리면 가장 긴 키워드를 따른다(쌀국수 > 국수, 타코야끼 > 타코). "샤브", "죽", "오리", "회"처럼 다른 단어에 섞이기 쉬운 말은 키워드로 쓰지 않는다.
- 이미 저장된 Google 출처 식당은 V23에서 같은 규칙으로 다시 분류했다.

### 식당 신고와 추천 제외 (2026-10-08)

- 추천받은 식당이 폐업했거나 정보가 틀린 경우 사용자가 `POST /api/v1/restaurants/{id}/reports`로 신고한다. 사유는 `CLOSED`·`WRONG_INFO`·`WRONG_CATEGORY`·`OTHER`, 메모는 선택(300자).
- 신고는 즉시 반영하지 않는다. 한 사람의 신고로 식당이 사라지면 장난·실수에 취약하고, 신고 수 기준 자동 제외는 사용자 수가 적은 지금은 기준을 잡기 어렵다. 운영자가 확인한 뒤 SQL로 처리한다(docs/OPERATIONS.md).
- 받아들인 신고는 식당 행에 `excluded_at`을 남기고 추천 후보 조회 SQL에서 뺀다. 식당 행을 지우지 않는 이유는 Pick·후기가 식당을 참조하고, Google 보충이 같은 Place ID를 다시 넣어도 제외 상태가 유지돼야 하기 때문이다.
- 같은 사람이 같은 식당을 검토 전에 다시 신고하면 409 `RESTAURANT_003`(부분 유니크 인덱스로도 막는다). 검토가 끝나면 다시 신고할 수 있다.
- 세션 안에서만 유효한 "다른 식당 보기"(제외 사유 `DISTANCE_TOO_FAR` 등)와는 별개다. 그쪽은 취향 피드백, 신고는 데이터 오류다.
