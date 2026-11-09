# restaurant 도메인

이 폴더 코드를 고칠 때 지킬 규칙이다. 자세한 결정과 기록은 [docs/domains/restaurant.md](../../../../../../../../docs/domains/restaurant.md), 공통 규칙은 루트 [AGENTS.md](../../../../../../../../AGENTS.md).

- Google Places 약관상 영구 저장은 `place_id`만, 좌표는 30일까지 허용된다. Google 필드를 새로 저장하는 코드를 추가하지 않는다(식당 DB 제거로 개편 예정, [recommendation.md](../../../../../../../../docs/domains/recommendation.md) "추천 구조 개편 결정").
- Google 데이터·사진을 응답에 담으면 출처를 같이 준다: `dataAttribution`("Google Maps"/null), 사진 촬영자는 `/photo/attributions`.
- Google 사진은 매 요청 Place Details → Place Photo로 받고(사진 이름 저장 금지) 30분만 메모리 캐시한다.
- CURATED 식당(탐색 스팟 25곳)은 Google 값으로 덮어쓰지 않는다.
- `FoodCategory`의 Google 유형 매핑은 [restaurant.md](../../../../../../../../docs/domains/restaurant.md) 표와 같게 유지한다.
- `GET /api/v1/restaurants/**`는 인증 없이 연다. 쓰기는 인증 필요.
- 식당 신고 검토 절차는 [OPERATIONS.md](../../../../../../../../docs/OPERATIONS.md).
