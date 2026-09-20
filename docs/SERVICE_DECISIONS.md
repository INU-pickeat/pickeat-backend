# Pick Eat 서비스 결정 사항

마지막 갱신일: 2026-09-20

## 제품 기준 문서

현재 Pick Eat 제품 기획이 기준(source of truth)이다. 예전 설문 서비스 문서와 과거 전시회 전용 식당 결정 사항은 역사적 참고용으로만 남겨둔다.

## 식당 데이터 전략

- 추천 후보는 Google Places 주변 검색(nearby search)에서 가져온다.
- 서비스는 안정적인 내부 ID, Pick, 후기, 외부 데이터 갱신에 필요한 최소한의 식당 스냅샷만 저장한다.
- `GOOGLE`과 `CURATED`는 `data_provider`로 구분해서 기록한다.
- 큐레이션 데이터는 초기 탐색 경험 전용이다: 연남·한남·서촌·신사·논현 각 5곳씩 총 25곳.
- Google 평점과 향후 Pick Eat 자체 평점은 의미와 갱신 정책이 달라서 분리해서 관리한다.

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

- 구현 완료: Member 회원가입·로그인, JWT 필터, Restaurant 영속성 모델, PostGIS 위치 컬럼과 인덱스, 동행 적합도 플래그, CI 데이터베이스 서비스, 식당 상세 조회 API, Google Places 클라이언트, Google primaryType 카테고리 매핑.
- 다음 작업: Google Restaurant upsert/갱신 정책, 5km 반경 레포지토리 조회, 외부 지도 링크 API, 추천 점수 계산과 세션 API.
