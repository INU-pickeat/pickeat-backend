# discovery 도메인

이 폴더 코드를 고칠 때 지킬 규칙이다. 자세한 결정과 기록은 [docs/domains/discovery.md](../../../../../../../../docs/domains/discovery.md), 공통 규칙은 루트 [AGENTS.md](../../../../../../../../AGENTS.md).

- 탐색 스팟은 운영자가 고른 고정 5개 지역 × 5곳(CURATED)이다. 사용자 행동 기반 "실시간 인기"라고 부르지 않는다.
- 식당과 순서는 Flyway 마이그레이션으로 시드하고, 원본은 `src/main/resources/curated/discovery-spots.csv`. DB와 CSV가 같은지 `RestaurantSchemaIntegrationTest`가 본다.
- 가짜 식당·임의 좌표는 운영 시드에 넣지 않는다.
