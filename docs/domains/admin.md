# 관리자

코드: `src/main/java/com/pickeat/pickeatbackend/domain/admin/`. 이 도메인의 현재 규칙과 결정 기록이다. 진행 현황은 [progress.md](../progress.md), API 명세 원본은 Notion BE > API SPECIFICATION이다.

## 결정 기록 (최신순)

구현 당시의 판단과 결과다. 현재 규칙과 다르면 현재 규칙이 우선이다.

### 느린 쿼리 TOP 10 (2026-11-27)

- 운영 상태 화면에 PostgreSQL `pg_stat_statements` 기준 평균 실행 시간이 긴 쿼리 10개(호출 수·평균·최대·합계 ms·행 수)를 붙였다. 조회·변경 문장만 보고 마이그레이션 DDL과 JDBC 드라이버 메타데이터 조회는 뺀다. Hibernate SELECT는 컬럼 목록을 `…`로 줄여 테이블이 보이게 한다. 바인딩 값은 `$1`처럼 가려진다.
- 확장은 V36 마이그레이션이 만들고, 통계 수집은 DB 서버가 `shared_preload_libraries`에 올려야 한다. 안 올라가 있으면(로컬 Postgres.app 등) 화면에 "꺼짐"으로 나오고 나머지 운영 상태는 정상 응답한다.
- 다른 운영 상태 값과 달리 DB에 쌓여 앱 재시작에는 남고, DB 재시작이나 `SELECT pg_stat_statements_reset()`으로 비워진다.
- 운영 적용(2026-11-27): `ALTER SYSTEM SET shared_preload_libraries = 'pg_stat_statements'` 후 컨테이너 재시작. DB가 약 3초 멈췄고 앱은 자동 재연결했다.

### 운영 상태 화면 (2026-11-27)

- 별도 모니터링 서버(Grafana·Sentry·Datadog) 대신 관리자 대시보드 아래에 "운영 상태"를 붙였다. t3.micro 메모리가 빠듯하고(908MB, 스왑 사용 중) 지금 규모에는 앱 안의 지표로 충분하다. 외부 모니터링은 다른 서비스에서 따로 운영한다.
- `GET /api/admin/system`: 가동 시간, JVM 힙, Hikari 커넥션(활성·유휴·대기·최대), 경로별 호출 수·5xx·평균·최대 응답 시간(호출 많은 순 20개, `/actuator` 제외), Google API 호출 수와 예상 비용, 최근 500 오류 50건.
- Google 비용: `GooglePlacesClient`가 과금 단위마다 `google.places.calls{api}` 카운터를 올리고, 1,000건당 단가(Nearby Search Enterprise+Atmosphere $40, Place Photo $7, Place Details Enterprise $20, 사진 이름 조회는 IDs Only라 $0)를 곱한다. 월 무료 한도는 빼지 않은 단순 환산이라 실제 청구는 Google Cloud 콘솔에서 확인한다. 사진 이름 조회의 IDs Only 등급 판단도 콘솔 SKU로 확인이 필요하다.
- 최근 오류: `GlobalExceptionHandler`의 500 처리에서 `RecentErrors`(메모리, 최신 50건, 메시지 300자)에 남긴다. 응답에는 여전히 내부 정보를 숨긴다.
- 한계: 값이 모두 서버 메모리에 있어 배포·재시작하면 0부터 센다. 기간 비교 그래프와 알림이 없다. 필요해지면 외부 수집기가 `/actuator/prometheus`를 읽게 한다(지금은 회원 인증 체인에 걸려 401).

### 관리자 대시보드 구현 결과 (2026-11-03)

- 사용자 테스트(8~15명)의 검증 질문 세 가지, 즉 결정이 빨라지는지, 추천 5곳이 만족스러운지, 기록·피드를 쓸 이유가 있는지를 보기 위한 화면이다. `GET /api/admin/stats?days=7`(1~90, 범위 밖은 맞춤)과 이를 그리는 `/admin.html` 한 장이다.
- 별도 이벤트 로그 테이블 없이 추천 세션·후보·제외·Pick·후기·좋아요 테이블을 집계한다. 기간은 한국 시간 기준 오늘을 포함한 최근 N일이다.
- 결정 소요 시간: Pick 시각에서, Pick 직전 30분 안이면서 Pick으로 이어진 세션보다 늦지 않은 같은 회원의 첫 추천 세션 시각을 뺀 값의 중앙값. 다시 추천하면 세션이 새로 생기기 때문에 묶어서 잰다. 조건 입력 화면에 머문 시간은 서버가 알 수 없어 과제형 테스트에서 직접 잰다.
- Pick 전환율 = Pick이 있는 세션 / 추천 세션. 재추천으로 버려진 세션도 분모에 들어가 실제보다 낮게 나온다.
- 추천 품질: Pick한 후보의 순위(6~10위는 제외 후 대체 후보), 제외 사유, 많이 노출된 식당 TOP 20과 식당별 Pick률. 노출은 처음 응답한 1~5위만 센다.
- 프랜차이즈: DB에 여부가 없어 추천과 같은 `FranchiseBrands`로 판정한다. 데이트 세션의 프랜차이즈 노출은 0이어야 정상이고, 목록에 없는 브랜드는 TOP 20을 보고 사람이 찾는다.
- 기록 사용: 후기 작성률(후기 / 취소 제외 Pick), 좋아요 수, 재방문 회원(기간 안에 2일 이상 추천을 요청한 회원).
- 테스터만 따로 구분하지 않는다. 테스트 기간으로 조회 기간을 맞춘다. 피드·식당 상세 조회 같은 GET 행동은 기록되지 않는다. 필요해지면 이벤트 수집 API를 추가한다.
- 인증은 회원 JWT와 별개인 HTTP Basic(`SecurityConfig.adminFilterChain`)이다. 계정은 `ADMIN_USERNAME`(기본 `admin`)/`ADMIN_PASSWORD`이다. 비밀번호는 기본값이 없어 없거나 비어 있으면 기동에 실패하고, `local` 프로필과 테스트만 `admin123`이다. 운영 설정은 [DEPLOYMENT.md](../DEPLOYMENT.md) 참고.
