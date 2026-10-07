# Pick Eat 서비스 결정 사항

마지막 갱신일: 2026-10-04

테스트 현황: 2026-10-04 `main`(`3009d49`) 기준 219개 통과. 아래 구현 결과에 적힌 개수는 각 작업 당시의 수치이며, 현재 수치는 CI 결과를 기준으로 한다.

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

- 최종 고정 지역: `신사(SINSA)`, `혜화(HYEHWA)`, `서촌(SEOCHEON)`, `한남(HANNAM)`, `종로(JONGNO)`.
- 지역별 식당은 정확히 5곳, 총 25곳으로 시작한다.
- 지역 목록은 초기 애플리케이션 계약에 고정하되 안정적인 지역 코드로 표현해서 이후 지역 추가가 가능하게 한다.
- 식당과 노출 순서는 소스 코드의 임시 객체가 아니라 Flyway 마이그레이션으로 DB에 직접 시드한다. 식당은 `data_provider=CURATED`로 구분한다.
- 동일 마이그레이션을 다시 적용해도 중복 식당이 생기지 않도록 안정적인 식별·중복 방지 기준을 함께 둔다.
- 초기 탐색 결과를 사용자 행동 기반의 "실시간 인기 순위"라고 표현하지 않는다. 현재 의미는 **운영자 선정 인기 후보**다.
- Pick·좋아요·후기 데이터와 집계 기능이 충분해지면 고정 목록을 동적 랭킹으로 교체한다. 그 전까지 자동 갱신·인기 점수·집계 배치는 구현하지 않는다.
- 실제 25개 식당과 지역 내 노출 순서는 2026-09-23 전달 목록으로 확정했다. 원본은 `src/main/resources/curated/discovery-spots.csv`로 보존한다.
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

## 추천 기본 정책

- 검색 반경은 수도권(서울·인천·경기)과 부산 1km(위경도 사각형 근사), 그 외 지역 5km이며 직선 거리를 사용한다. 거리 점수 정규화도 같은 반경을 쓴다(2026-09-28).
- 후보가 5개 미만이어도 반경을 자동으로 넓히지 않는다.
- 조회는 `RestaurantRepository.findWithinRadius()`(M1-4, 2026-09-20)가 담당한다. `ST_DWithin(location, point, radius)`로 GiST 인덱스(`restaurants_location_gist_idx`)를 태울 수 있는 형태를 쓰고, 거리순 정렬 결과를 반환한다. 반경은 호출자가 넘긴 값 그대로만 쓰고 메서드 내부에서 넓히지 않는다.
- 음식 카테고리는 한식·일식·중식·양식·카페/디저트·펍/와인/술집·기타 7종이다.
- 요청에는 음식 카테고리를 1개 이상, 동행 유형은 정확히 1개 포함할 수 있다.
- 동행 유형은 데이트·가족·아이·혼밥·단체·반려견 6종이다. 기존 `FRIENDS`는 별도 유형으로 유지하지 않고 `GROUP`에 포함한다.
- 사용자가 가격 범위를 지정하면 식당의 Google `priceRange`와 일부라도 겹치는 후보만 포함한다. `priceRange` 필드를 생략하면 가격 무관이며 가격 정보가 없는 식당도 포함한다. 명시적 `null`도 호환상 가격 무관으로 처리하되 API 예제는 필드 생략을 표준으로 사용한다.
- 동행 적합도는 제외 필터가 아니라 점수 가산 요소다.
- 목표 결과 개수는 5개다. 조건을 만족하는 후보가 5개보다 적으면 반경을 넓히지 않고 조회된 후보만 반환한다.

## API와 운영

- 인증 없이 접근 가능한 엔드포인트는 `/api/v1/auth/email-verifications`, `/api/v1/auth/email-verifications/confirm`, `/api/v1/auth/signup`, `/api/v1/auth/login`, `/api/v1/auth/refresh`, `/api/v1/auth/logout`, `GET /api/v1/discovery-spots`, `GET /images/discovery/**`이다.
- 그 외 모든 비즈니스 엔드포인트는 명시적으로 문서화되지 않는 한 인증(bearer token)이 필요하다.
- 예외: `GET /api/v1/restaurants/**`는 인증 없이 접근 가능하다. 식당 상세는 민감 정보가 아니고, 공유 링크는 비로그인 사용자도 열 수 있어야 한다. 식당별 후기 요약(`GET /api/v1/restaurants/{id}/review-summary`)도 이 경로 아래라 인증 없이 조회한다. 식당 관련 쓰기/변경 작업은 여전히 인증이 필요하다.
- OpenAPI와 Swagger UI는 `local` Spring 프로필에서만 활성화된다.
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

## 구현 현황

- 기반 구현 완료: Member 회원가입·로그인, JWT 필터, Restaurant 영속성 모델, PostGIS 위치 컬럼과 인덱스, 기존 동행 적합도 플래그, CI 데이터베이스 서비스, 식당 상세 조회 API, Google Places 클라이언트, Google Restaurant upsert·갱신 정책, PostGIS 반경 후보 조회, 외부 지도 링크 API. 기존 **M1 Restaurant 마일스톤은 2026-09-20 완료**했으며, 2026-09-22 기획의 카테고리·동행 신호·가격 필드 확장은 별도 후속 작업으로 다시 열었다.
- M2 작업은 아래 순서로 진행한다. 완료된 항목은 체크하고, 항목을 마칠 때마다 이 목록을 갱신한다.
  1. [x] 문서 계약 정리: Notion API 상태, README 로컬 실행법, 초기 탐색 스팟 정책을 동기화한다. (PR #24)
  2. [x] M2 ADR 확정: 추천 후보 조회 방식과 평점 보정·거리 정규화·동행 가산점 수치를 결정한다. → 아래 "M2 ADR 확정 결과" 참고.
  3. [x] M2-1: 추천 요청 DTO와 음식 카테고리·동행·위경도 입력 검증을 구현한다. (PR #25)
  4. [x] M2-2: RecommendationSession과 후보·점수 구성 요소 스키마를 설계하고 마이그레이션한다. (PR #27)
  5. [x] M2-3: ADR에 따른 점수 계산기와 경계값 테스트를 구현한다. (PR #26)
  6. [x] M2-4~5: Top 5 추천 서비스와 추천 생성·세션 조회 API를 완성한다. (PR #28)
  7. [ ] **M2 계약 개편.** 7개 음식 카테고리, 6개 동행 유형, 가격대 필터, 새 점수 공식, 재추천·제외 사유, 응답 계약을 반영한다.
     - [x] 가격대 필터 + DB 우선·Google 보충 하이브리드 조회. → 아래 "M2 가격 필터 + DB 우선 하이브리드 구현 결과" 참고.
     - [x] 새 점수 공식(평점 정규화 3.0~5.0 클램프, 0.7/0.3, 가산점 0.1). → 아래 "M2 점수 공식 재구현 결과" 참고.
     - [x] 재추천(다시 추천) + 제외 사유. → 아래 "M2 재추천 + 제외 사유 구현 결과" 참고.
     - [x] 데이트 프랜차이즈 제외(2026-10-04). → 아래 "데이트 프랜차이즈 제외 구현 결과" 참고.
     - [ ] 제네릭 `restaurant` 보완 분류
  8. [x] **M3 계약 개편.** Pick 동행 스냅샷, 기간별 식당 집계 목록, REVIEWED 전용 지도를 반영했다. 기존 생성·상태 전이·권한 검증은 유지한다. Pick 캘린더는 2026-10-04에 구현했다(#39). → "M3 계약 개편 구현 결과", "Pick 캘린더 구현 결과" 참고.
  9. [x] **M2.5: 초기 탐색 스팟.** V13에서 최종 5개 지역, 지역-식당 관계 스키마와 공개 탐색 조회 API를 구현했다. Google Places Text Search로 25개 식당의 Place ID·좌표를 검증하고 V14로 Restaurant와 지역별 고정 순서를 시드했다. 사진은 JPG 수령 후 URL만 후속 반영한다.
  10. [x] **배포/CD.** GitHub Actions CD·systemd·헬스체크·자동 롤백 구성, t3.micro 단일 인스턴스 구성(swap·PostGIS Docker·Nginx·백업 cron·JVM 힙 제한), EC2 생성, Secrets 등록, 최초 실배포(2026-10-01), DNS·HTTPS 연결(2026-10-04)을 마쳤다. 자동 롤백을 실제 실패 배포로 검증하는 일만 남았다. → "운영 배포 결과" 참고.
  11. [ ] 프론트엔드 연동 MVP E2E를 검증한다. 백엔드 단독 E2E(회원가입 → 추천 → 다시 추천 → Pick → 로그아웃)는 운영 HTTPS 기준으로 통과했다(2026-10-04).
  12. [x] **Refresh Token.** 로그인 시 refresh token을 함께 발급하고, 재발급(rotation)·로그아웃(폐기) API를 추가했다. → 아래 "Refresh Token 구현 결과" 참고.
  13. [ ] 프로필 이미지 업로드 — 후기 이미지와 같은 S3 presigned URL 방식을 쓰기로 했다. 구현은 아직이다.
  14. [x] **지역별 검색 반경.** 수도권·부산은 1km, 그 외 지역은 5km로 바꿨다. → 아래 "지역별 검색 반경 구현 결과" 참고.
  15. [x] **S3 도입 범위 결정.** (B) 사용자 업로드를 골랐고 후기 이미지부터 presigned URL로 도입했다(2026-10-04). 탐색 스팟 JPG 25장은 계속 JAR에서 제공한다. 버킷·IAM·CORS 설정은 `docs/DEPLOYMENT.md` 참고.
  16. [ ] **(보류) 반경 판정 정확도.** 사각형 근사로 생기는 경계 오판(춘천 서부·철원·김해·양산 일부는 1km, 양평·가평·여주 동쪽 끝과 인천 먼 섬은 5km)이 실제 문제가 되면 행정구역 폴리곤이나 역지오코딩으로 교체한다.
  17. [ ] **(보류) 1km 반경 거리 가중치.** 실사용 데이터에서 1km 안의 거리 차이가 선택에 영향이 없다고 확인되면 거리 가중치(0.3) 하향을 기획서에서 검토한다. 그 전에는 ADR에 따라 상수를 바꾸지 않는다.

  18. [x] **Review·공개 피드·좋아요·후기 요약.** 후기 작성·조회·수정·삭제, 이미지 업로드 URL, 공개 피드, 공개 후기 좋아요, 식당별 후기 요약을 구현했다(V19). → "Review·피드 구현 결과" 참고.
  19. [ ] **(보류) 동적 지역별 인기맛집.** 후기·좋아요 데이터가 쌓인 뒤 진행한다. 그 전까지 탐색 스팟은 운영자 선정 25곳을 유지한다.
  20. [x] **(제외) 카테고리·동행 유형 조회 API.** 별도 조회 API를 만들지 않는다. 후기 작성 화면에서 사용자가 고른 값을 후기 저장 요청에 함께 받는다(2026-10-04).
  21. [ ] **후기 한줄평 선정 규칙 확정.** 지금은 식당별 가장 최근 공개 후기의 첫 줄(최대 50자)이다. 좋아요 수 등 다른 기준이 정해지면 `ReviewSummaryService`만 바꾼다.
  22. [x] **회원가입 이메일 인증.** 인증번호 발송·확인 API를 추가하고 인증을 마친 이메일만 가입할 수 있게 했다(V20). → 아래 "이메일 인증 구현 결과" 참고.
  23. [ ] **이메일 인증 운영 발송 검증.** 운영 서버 `MAIL_PASSWORD`(Gmail 앱 비밀번호)를 설정하고 실제 메일 수신을 확인한다.

### 이메일 인증 구현 결과 (2026-10-07)

- 흐름은 `POST /api/v1/auth/email-verifications`(발송) → `POST /api/v1/auth/email-verifications/confirm`(확인) → 기존 `POST /api/v1/auth/signup`이다. 회원가입 요청 본문은 바꾸지 않았다. 두 인증 API는 인증 없이 호출하며 성공 시 204를 반환한다.
- 인증번호는 `SecureRandom` 6자리 숫자이고 DB(`email_verifications`, V20)에는 BCrypt 해시만 저장한다. 유효 10분, 재발송 1분 제한, 실패 5회 초과 시 거부, 인증 후 30분 안에 가입해야 한다. 가입에 성공하면 인증 정보를 삭제해 재사용을 막는다.
- 이메일은 발송·확인·가입·로그인 모두 `strip()` + 소문자로 정규화한다.
- 오류 코드: `MEMBER_005`(403, 미인증), `MEMBER_006`(400, 잘못된 번호), `MEMBER_007`(410, 만료), `MEMBER_008`(429, 재발송 제한), `MEMBER_009`(503, 메일 발송 실패). 이미 가입된 이메일은 기존 `MEMBER_001`(409).
- 발신 계정은 Gmail SMTP(`cki08543@gmail.com`)다. 운영 서버에 `MAIL_PASSWORD`로 앱 비밀번호를 넣어야 실제 발송된다.

### M2 ADR 확정 결과 (2026-09-21, 2026-09-22 개편으로 일부 폐기)

- **점수 계산 공식:** Google 평점을 `(rating - 3.0) / 2.0`으로 정규화하고 0~1로 제한한다. 거리 점수는 `1 - distance/5000`, 가중치는 `rating=0.7`, `distance=0.3`, 동행 적합 가산점은 `0.1`이다. 기존 `rating/5`, `0.6/0.4` 공식은 폐기했다(구현 완료, 아래 "M2 점수 공식 재구현 결과" 참고).
- **추천 후보 조회 방식(목표 계약):** DB에서 5km 이내 유효 후보를 먼저 조회하고, 카테고리·가격·데이트 프랜차이즈 필터를 통과한 후보가 10개 미만일 때만 Google Places로 보충한다. 상위 5개는 노출하고 나머지 5개는 대체 후보로 유지한다. 현재 `RecommendationService`의 매 요청 Google 호출은 후속 M2 개편에서 이 흐름으로 교체한다.

### M2-4~5 구현 결과 (2026-09-21)

- `POST /api/v1/recommendations`(추천 생성) / `GET /api/v1/recommendations/{sessionId}`(세션 조회) 추가. 둘 다 인증 필요, 세션 조회는 요청자가 세션 소유자가 아니면 404.
- 추천 후보는 Google Nearby Search 응답을 upsert한 뒤 `RestaurantRepository.findWithinRadius()`로 다시 조회해 거리를 얻는다 — CURATED 데이터가 생기는 M2.5 이후에도 같은 조회로 자동 포함된다.
- 후보가 0개면 빈 `items`, 1~4개면 조회된 개수만, 5개를 초과하면 점수순 상위 5개만 반환하는 경계 테스트를 고정했다.
- 추천 생성·세션 조회의 JWT 인증, HTTP 상태, 요청 검증 오류(`GLOBAL_001`), JSON 응답 필드를 MockMvc 계약 테스트로 검증한다.
- 당시 기준 전체 테스트 134개가 통과했다.

초기 탐색 스팟의 지역·개수·25개 식당·노출 순서는 2026-09-23 확정했다. 식당별 좌표와 Google Place ID를 Google Places Text Search로 검증하고 V14에서 실제 Restaurant와 지역 관계 데이터를 시드했다. 서촌 `팔`은 일반 명사 검색의 첫 결과가 오매칭되어 `카페 팔 서촌` 재검색과 주소 대조로 Google 표기 `PHAL`을 확정했다.

### M2 가격 필터 + DB 우선 하이브리드 구현 결과 (2026-09-22)

- `RecommendationRequest`에 nullable `priceRange(min, max)`를 추가했다. 생략·명시적 `null`은 가격 무관으로 처리하고, 값이 있으면 `min`/`max` 각각 음수를 금지하고(`@PositiveOrZero`) `min > max`는 레코드 내부 `@AssertTrue`로 거부한다. 위반 시 기존 `GLOBAL_001` 계약을 그대로 재사용한다.
- 가격 무관이면 가격 정보가 없는 식당도 포함하고, 가격을 지정하면 가격 정보가 없는 식당은 제외한다. 식당 쪽에 상한 또는 하한만 있으면 그 방향은 무제한으로 보고, 요청 범위와 일부라도 겹치면 포함한다(`RecommendationService.matchesPriceRange()`).
- 조회 흐름을 "DB 우선, 부족할 때만 Google"로 교체했다. DB에서 5km 이내 후보를 조회해 카테고리·가격 필터를 통과한 유효 후보가 10개 미만일 때만 Google Places를 호출해 upsert한 뒤 DB를 다시 조회한다. 10개 이상이면 Google을 호출하지 않는다.
- 점수순 상위 10개를 세션 후보로 저장한다(1~5위는 응답에 노출, 6~10위는 추후 제외 API를 위한 대체 후보로만 보관). 추천 생성·세션 조회 API 응답에는 상위 5개만 반환한다.
- `RecommendationSession`에 요청 당시 `price_range_min`/`price_range_max`를 저장한다(V10). `recommendation_candidates.result_rank` 제약을 1~5에서 1~10으로 넓혔다.
- 프랜차이즈 브랜드 목록이 아직 없어 데이트 프랜차이즈 제외 필터와 제네릭 `restaurant` 보완 분류, 새 점수 공식(0.7/0.3 가중치)은 이번 작업 범위에서 제외했다.
- 단위·API 계약·Flyway 통합 테스트를 추가해 당시 기준 전체 테스트 164개가 통과했다.

### M2 점수 공식 재구현 결과 (2026-09-22)

- `RecommendationScoreCalculator`를 ADR 확정본으로 교체했다. 평점은 `(rating - 3.0) / 2.0`으로 정규화한 뒤 `Math.clamp`로 0~1에 제한하고(3.0 미만은 0점, 5.0은 만점), 거리 점수(`1 - distance/5000`)는 그대로 두되 가중치를 `rating=0.7`, `distance=0.3`으로 바꿨다. 동행 적합 가산점 `0.1`은 유지한다.
- 평점이 없으면(NULL) 기존과 동일하게 평점 항을 0으로 계산한다.
- `RecommendationScoreCalculatorTest`에 평점 3.0 미만이 3.0과 동일하게 0점 처리되는 클램프 경계 테스트를 추가했다.
- 당시 기준 전체 테스트 165개가 통과했다.

### M2 재추천 + 제외 사유 구현 결과 (2026-09-22)

- `POST /api/v1/recommendations/{sessionId}/exclusions` API를 추가했다. 요청은 `restaurantId`와 `reason`(`DISTANCE_TOO_FAR`/`PRICE_TOO_HIGH`/`MENU_UNSATISFACTORY`/`ATMOSPHERE_MISMATCH`/`WANT_DIFFERENT`) 둘 다 필수이며, 응답은 대체 후보가 반영된 `RecommendationResponse`(상위 5개)다. 인증 필요, 세션 소유자가 아니면 404.
- `RecommendationExclusion` 엔티티(세션·식당·사유·제외 일시)와 V11 마이그레이션을 추가했다. `(session_id, restaurant_id)` 유니크 제약으로 같은 세션 내 중복 제외를 막는다.
- 제외 대상이 해당 세션의 저장된 후보(1~10위)가 아니면 `RECOMMENDATION_002`(404)로 거부한다. 이미 제외한 식당을 다시 제외하면 새 레코드를 만들지 않고 멱등하게 현재 상태만 반환한다.
- 노출 목록은 세션에 저장된 1~10위 후보 중 제외되지 않은 것만 순서대로 최대 5개 뽑아 구성한다. 대체 후보가 소진되면(10위까지 다 제외) 반경을 넓히지 않고 5개보다 적게 노출한다.
- 제외는 해당 세션 안에서만 유효하다(영구 차단 아님) — 다른 세션이나 이후 추천에는 영향을 주지 않는다.
- `RecommendationResponse.Item.rank`의 의미를 세션 저장 순위(1~10, 불변)에서 현재 노출 목록 안에서의 위치(항상 1부터 연속)로 바꿨다. 제외로 빈 자리가 생기면 다음 대체 후보가 그 자리의 순위를 그대로 이어받는다. `recommend`/`getSession` 기존 응답 계약은 동일하게 유지된다(제외 이력이 없으면 결과가 같다).
- 단위·API 계약·Flyway 통합 테스트를 추가해 당시 기준 전체 테스트 180개가 통과했다.

### M3 계약 개편 구현 결과 (2026-09-22)

- **Pick 동행 스냅샷.** `Pick`에 `companion_type` 컬럼을 추가하고(V12), 생성 시 추천 세션의 `companionType`을 그대로 복사해 저장한다. 이후 세션이 바뀌어도 이미 생성된 Pick의 값은 변하지 않는다. 기존 행은 연결된 세션의 현재 `companion_type`으로 백필했다.
- **Pick 지도 REVIEWED 전용.** 기존 "CANCELED만 제외"에서 "REVIEWED만 노출"로 바꿨다(`findByMemberIdAndStatusOrderBySelectedAtDescIdDesc`). `PickMapResponse.Item`에 스냅샷된 `companionType`을 추가해 지도 팝업에 표시할 수 있게 했다. Review 기능이 아직 없어 REVIEWED Pick이 생기기 전까지는 지도 결과가 비어 있는 것이 정상이다.
- **최근 Pick 목록 개편.** `GET /api/v1/me/picks`의 계약을 페이지 조회에서 `period=week|month` 필수 쿼리 파라미터 기반 식당별 집계로 완전히 교체했다(기존 `page`/`size`, `PickListResponse`는 제거). SELECTED + REVIEWED만 집계하고 CANCELED는 제외하며, 식당별로 `pickCount`와 `latestPickedAt`을 반환한다. `period`는 `week`(최근 7일)·`month`(최근 30일) 롤링 윈도우로 해석했다 — 기획서에 "일주일 기준"/"한 달 기준"의 정확한 경계(캘린더 월 vs 롤링 30일)가 명시되어 있지 않아 내린 구현 판단이며, 기획자 확인이 필요하면 조정한다. `period`가 `week`/`month`가 아니면 `GLOBAL_001`로 거부한다.
- **Pick 캘린더는 당시 범위 밖.** 대표 이미지 출처가 리뷰 사진인데 Review 모듈이 없어 이 작업에서는 구현하지 않았다. 2026-10-04에 구현했다 — "Pick 캘린더 구현 결과" 참고.
- 단위·API 계약·Flyway 통합 테스트를 갱신해 당시 기준 전체 테스트 184개가 통과했다.

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

### 지역별 검색 반경 구현 결과 (2026-09-28)

- `RecommendationService.searchRadiusMeters(lat, lng)`가 수도권(위도 36.89~38.30, 경도 126.30~127.70)·부산(위도 34.88~35.39, 경도 128.76~129.31)이면 1000m, 그 외는 5000m를 돌려준다.
- DB 반경 조회, Google Nearby Search `radius`, 거리 점수 정규화(`1 - 거리/반경`)가 모두 같은 값을 쓴다. 정규화가 반경 기준이라 점수 범위(거리 0~0.3)와 가중치 0.7/0.3/+0.1은 그대로 유지된다.
- 행정구역을 조회하지 않는 위경도 사각형 근사다. 경계 오판은 구현 현황 16번 참고.
- `recommendation_candidates.distance_meters` CHECK(0~5000)는 최대 반경과 같아 변경하지 않았다.

### Pick 캘린더 구현 결과 (2026-10-04)

- `GET /api/v1/me/picks/calendar?year&month`. REVIEWED만 방문일(`visited_at`)을 한국 시간 날짜로 묶어 `recordCount`, 그날 첫 기록의 `restaurantName`, `representativeImageUrl`을 돌려준다.
- 월 경계도 한국 시간 기준이다. 연 2000~2100·월 1~12 밖이거나 누락·숫자 아님이면 `GLOBAL_001`이다. 쿼리 파라미터 누락·타입 오류를 공통 예외 핸들러에서 `GLOBAL_001`로 응답하도록 함께 바꿨다.
- 대표 이미지는 그날 가장 먼저 쓴 후기 중 사진이 있는 첫 후기의 첫 번째(0번) 사진이다. 그날 후기에 사진이 하나도 없으면 `null`이다.

### Review·피드 구현 결과 (2026-10-04)

- **후기.** `POST /api/v1/reviews`, `GET`/`PATCH`/`DELETE /api/v1/reviews/{reviewId}`. 후기는 Pick 하나당 하나이며(`REVIEW_002`), 작성하면 그 Pick이 `REVIEWED`가 된다. 취소한 Pick에는 쓸 수 없다(`PICK_004`). 삭제하면 Pick이 `SELECTED`로 돌아가 지도·캘린더에서 빠지고 다시 후기를 쓸 수 있다.
- **입력값.** 별점 1~5, 내용 1~1000자, 음식 카테고리, 동행 유형, 공개 범위(`PUBLIC`/`PRIVATE`), 이미지 URL 최대 1개(`imageUrls` 배열로 받되 지금은 한 장만 허용한다). 음식 카테고리와 동행 유형은 추천 세션 값이 아니라 후기 작성 화면에서 사용자가 직접 고른 값이다. 그래서 선택지 조회 API는 따로 만들지 않았다.
- **이미지.** `POST /api/v1/reviews/images/upload-urls`에 Content-Type 목록을 보내면 10분짜리 S3 presigned PUT URL과 저장될 `imageUrl`을 돌려준다. JPEG·PNG·WebP만 허용한다(`REVIEW_005`). 후기에는 본인 경로(`reviews/{memberId}/`) 아래의 URL만 붙일 수 있다(`REVIEW_004`). 버킷이 설정되지 않았으면 발급을 `REVIEW_006`(503)으로 거부한다. presigned PUT은 파일 크기를 제한하지 못하고, 후기에서 뺀 이미지의 S3 객체는 지우지 않는다.
- **공개 피드.** `GET /api/v1/feed?cursor&size`. `PUBLIC` 후기만 최신순으로 내려준다. 커서는 이전 응답의 `nextCursor`(마지막 `reviewId`)이고 `null`이면 마지막 페이지다. 크기는 1~50, 기본 20이다. 인증이 필요하며 항목마다 `likeCount`와 `likedByMe`를 준다.
- **좋아요.** `POST`/`DELETE /api/v1/reviews/{reviewId}/likes`. 공개 후기에만 누를 수 있다. 본인의 비공개 후기는 `REVIEW_003`(409), 남의 비공개 후기는 존재를 알리지 않도록 `REVIEW_001`(404)이다. 여러 번 눌러도 한 번으로 세며, 동시 요청은 DB 유니크 제약과 `ON CONFLICT DO NOTHING`으로 처리한다.
- **식당별 후기 요약.** `GET /api/v1/restaurants/{restaurantId}/review-summary`. 공개 후기 수, 평균 별점(소수 첫째 자리), 대표 한줄평을 돌려준다. 인증이 필요 없다. 후기가 없으면 0건·평균 `null`·`"후기가 없습니다."`다.
- **한줄평.** 탐색 스팟의 `oneLineIntro`와 후기 요약의 `oneLineReview`는 같은 규칙을 쓴다. 식당별 가장 최근 공개 후기의 첫 줄이며 50자를 넘으면 잘라 말줄임표를 붙인다. 운영자 소개 문구(`discovery_spot_restaurants.one_line_intro`)는 더 이상 API로 내려가지 않는다. 선정 규칙은 임시이며 구현 현황 21번에서 확정한다.
- **스키마.** V19에 `reviews`, `review_images`, `review_likes`를 추가했다. 이미지와 좋아요는 후기 삭제 시 함께 지워진다.

### 운영 배포 결과 (2026-10-01 ~ 2026-10-04)

- 2026-10-01: EC2(t3.micro, Elastic IP `3.39.48.167`)에 최초 배포. GitHub Actions의 SSH가 타임아웃되던 문제는 보안 그룹 22번 인바운드가 관리자 IP `/32`로만 열려 있던 탓이었고, `0.0.0.0/0`으로 열고 fail2ban을 적용해 자동 CD가 동작한다.
- 2026-10-04: `api.pickeat.kr` DNS 연결, Nginx `server_name` 변경, Let's Encrypt 인증서 발급(만료 2027-01-02, 자동 갱신 모의 실행 성공). HTTP는 301로 HTTPS에 리다이렉트한다. CORS는 `https://www.pickeat.kr` 허용을 확인했다.
- 운영 E2E 스모크 테스트(`pickeat-e2e.sh`)가 HTTPS 기준으로 통과했다. DB 후보가 부족한 지역의 첫 추천은 Google 보충으로 약 1.6초, DB만 쓰면 100ms 안쪽이었다.
- 남은 운영 작업: 자동 롤백 실검증, 패키지 업데이트 후 재부팅, 후기 이미지용 S3 버킷·IAM 역할 생성.

### 2026-09-22 기획서 갱신 — 구현 필요 백로그

Notion 기획서가 큰 폭으로 갱신됐다. 아래는 현재 코드(M1~M3, 134개 테스트 통과 시점)와 스펙 사이 gap이다. 문서 정합성을 먼저 맞춘 뒤 M1 데이터 확장 → M2 계약 개편 → M3 계약 개편 순서로 진행한다.

- [x] **음식 카테고리 5종 → 7종.** `PUB_BAR`, `OTHER`를 추가하고 일식·중식·양식·펍 매핑과 기타 허용 목록을 확장했다. 카테고리 검색은 `includedPrimaryTypes`를 사용하며 50개 초과 시 요청을 분할·중복 제거한다. 제네릭 `restaurant` 보완 분류는 별도 후속 과제로 유지한다.
- [x] **동행 유형 5종 → 6종.** `DATE`, `FAMILY`, `CHILDREN`, `SOLO`, `GROUP`, `DOG`로 개편했다. Google 신호는 가족=goodForChildren AND goodForGroups, 아이=goodForChildren OR menuForChildren, 단체=goodForGroups, 반려견=allowsDogs로 채우며 기존 수동 값은 덮어쓰지 않는다.
- [x] **가격대(신규 선택 필터).** V9의 `priceRange` 금액·통화 저장에 이어, 추천 요청 DTO의 생략/NULL 계약과 일부 겹침 필터까지 구현했다. → "M2 가격 필터 + DB 우선 하이브리드 구현 결과" 참고.
- [x] **추천 점수 공식 재구현.** 기존 구현(원시 평점, w1=0.6/w2=0.4)을 스펙 확정본(평점 정규화 3.0~5.0 클램프, 0.7/0.3, 가산점 0.1)으로 교체했다.
- [x] **재추천(다시 추천) + 제외 사유.** `RecommendationExclusion` 엔티티, enum(`DISTANCE_TOO_FAR`/`PRICE_TOO_HIGH`/`MENU_UNSATISFACTORY`/`ATMOSPHERE_MISMATCH`/`WANT_DIFFERENT`), `POST /api/v1/recommendations/{sessionId}/exclusions`. → "M2 재추천 + 제외 사유 구현 결과" 참고.
- [x] **Pick 지도.** REVIEWED만 노출하도록 반영했다. 캘린더는 2026-10-04에 구현했다. 지도·캘린더는 후기를 작성한 Pick만 보여 주므로 후기가 없으면 비어 있는 것이 정상이다. → "M3 계약 개편 구현 결과" 참고.
- [x] **최근 Pick 목록.** `GET /api/v1/me/picks?period=week|month` — SELECTED + REVIEWED를 식당별로 그룹화하고 `pickCount`와 `latestPickedAt`을 제공한다. CANCELED는 제외한다.
- [x] **Pick 동행 스냅샷.** Pick 생성 시 추천 세션의 동행 유형을 Pick에 복사해 이후 변경과 무관한 기록으로 보존한다.
- [x] **Member 프로필 확장.** 자기소개(bio) 필드, `GET /api/v1/me`, `PATCH /api/v1/me`를 사용한다. 다른 사용자용 리소스가 필요할 때만 `/api/v1/members/{memberId}`를 추가한다. → 아래 "Member 프로필 구현 결과" 참고.
- [ ] **세션 ID 유지.** 추천 세션의 내부·API 식별자는 현재의 `Long`을 유지한다. 외부 공유가 필요해지면 별도 UUID 공개 식별자를 추가한다.
- [x] **인기맛집 지역 확정.** 기존 후보 지역과 M1 검증 지역을 대체해 `신사·혜화·서촌·한남·종로`를 최종 5개 탐색 스팟으로 확정했다.

각 항목을 시작하기 전 Notion "Pick Eat 프로젝트 기획서" 최신 상태를 다시 확인한다(수치가 재조정될 수 있다고 스펙에 명시됨).

### M2.5 진행 기준

- 홈 탐색 경험에 필요하므로 제품 우선도는 높다.
- V13에서 지역 코드·스키마와 탐색 조회 API를 구현하고 최종 25개 목록을 CSV 원본으로 고정했다.
- 추천 반경 계산에 필요한 위도·경도와 중복 방지용 Google Place ID는 Google Places에서 검증한 뒤 V14로 등록했다. 25곳 모두 `data_provider=CURATED`이며 기존 Google 레코드가 있으면 같은 Place ID 행을 큐레이션 데이터로 갱신한다.
- 사진은 `representative_image_url=NULL`을 허용하므로 JPG 수령 전에도 나머지 작업을 진행한다.

### M3 Pick 구현 결과 (2026-09-21)

- 추천 세션당 최종 Pick은 하나만 허용한다. 같은 식당은 서로 다른 추천 세션에서 다시 Pick할 수 있다.
- Pick 대상은 요청한 회원이 소유한 추천 세션에 실제 후보로 저장된 식당이어야 한다. 다른 회원의 세션·Pick은 404로 처리해 존재 여부를 노출하지 않는다.
- 상태는 `SELECTED`, `REVIEWED`, `CANCELED` 세 가지다. `SELECTED`에서 `REVIEWED` 또는 `CANCELED`로만 전환하며, 같은 상태 요청은 멱등 처리하고 종료 상태는 되돌리지 않는다.
- 2026-10-04부터 `REVIEWED` 전환은 후기 작성(`POST /api/v1/reviews`)으로만 일어난다. `PATCH /api/v1/picks/{pickId}`로 `REVIEWED`를 요청하면 `PICK_005`(400)이고, 이 API로는 취소만 할 수 있다. 후기를 삭제하면 Pick은 `SELECTED`로 돌아간다.
- `REVIEWED` 전환 시 `visited_at`을 기록한다. (2026-09-22 개편으로 지도는 REVIEWED 전용, 목록은 기간별 집계로 바뀌었다 — 아래 "M3 계약 개편 구현 결과" 참고.)
- API는 `POST /api/v1/picks`, `PATCH /api/v1/picks/{pickId}`, `GET /api/v1/me/picks`, `GET /api/v1/me/picks/map`, `GET /api/v1/me/picks/calendar`(2026-10-04 추가) 다섯 개이며 모두 JWT 인증이 필요하다.
- Flyway V8에 `picks` 테이블, 추천 세션 유일 제약, 회원별 최신순·상태·식당 인덱스를 추가했다.
- 엔티티 상태 전이, 서비스 권한·중복·후보 검증, DB 제약, JWT 및 JSON 계약을 포함해 당시 기준 전체 테스트 134개가 통과했다.

## 데이트 프랜차이즈 제외 구현 결과 (2026-10-04)

- 동행 유형이 `DATE`인 추천에서만 프랜차이즈 식당을 후보에서 뺀다. 다른 동행 유형에는 적용하지 않는다.
- Google Places에는 프랜차이즈 여부 필드가 없어서 팀이 정리한 브랜드 목록(`src/main/resources/curated/franchise-brands.txt`)과 식당 이름을 비교한다(`FranchiseBrands.isFranchise()`).
- 판별 규칙: 공백·기호를 지우고 소문자로 맞춘 뒤, 4글자 이상 브랜드는 식당 이름이 그 브랜드로 시작하면, 3글자 이하 브랜드(본가, 두끼, KFC 등)는 이름의 첫 단어가 브랜드와 같을 때만 프랜차이즈로 본다.
- 프랜차이즈지만 맛집으로 인식될 수 있는 폴바셋, 고든램지 스트리트 버거, 깐부치킨, 청기와타운, 미카도스시, 상무초밥, 은행골은 제외하지 않는다(목록에 넣지 않았다).
- 필터는 Google 보충 판단 전에 적용한다. 프랜차이즈를 뺀 유효 후보가 10개 미만이면 Google Places로 보충한다.
- 한계: 이름 비교라서 "본가 ○○"처럼 짧은 브랜드와 첫 단어가 같은 개인 식당은 함께 빠지고, 목록에 없는 표기(영문명, 변형 표기)는 걸러지지 않는다. 브랜드를 추가·삭제하려면 목록 파일만 고치면 된다.

## Google 보충 기준 카테고리별 전환 (2026-10-04)

- 문제: 보충 기준이 "유효 후보 합계 10개 미만"뿐이라, 한 카테고리만 DB에 쌓인 지역에서는 여러 카테고리를 요청해도 그 카테고리만 추천됐다. 운영에서 홍대입구역 기준 7개 카테고리를 요청했을 때 5곳이 모두 일식이었다(당시 DB 62곳 중 일식 40곳).
- 변경: 합계가 10개 미만이면 요청한 카테고리 전부를, 합계가 10개 이상이면 유효 후보가 5개 미만인 카테고리만 Google Places로 보충한다(`RecommendationService.categoriesToFill()`).
- 같은 구역(위경도 0.01도 격자, 약 1km)·카테고리는 한 번 보충하면 6시간 동안 다시 호출하지 않는다. 식당이 원래 적은 구역에서 요청마다 Google을 부르는 것을 막기 위해서다.
- 한계: 보충 기록은 인스턴스 메모리에 있어서 재시작하면 비워지고, 인스턴스가 여러 대가 되면 공유되지 않는다. 같은 격자 안이라도 위치가 다르면 반경에 들어오는 식당이 달라 6시간 동안 보충이 덜 될 수 있다.
- 점수 계산은 그대로다. 카테고리별로 고르게 뽑지는 않으므로, 보충 후에도 평점·거리가 좋은 한 카테고리가 상위 5곳을 차지할 수 있다.
