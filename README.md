# Pick Eat Backend

Java 21 / Spring Boot 4.1.1 / PostgreSQL 기반 백엔드.

확장 서비스는 외부 Places API로 주변 식당을 조회하며, 검색 결과 전체를 DB에 저장하지 않는다.
현재 식당 테이블과 Repository는 혜화 전시용 수동 데이터 경로다.
최신 추천 흐름과 저장 범위는 [서비스 결정](docs/SERVICE_DECISIONS.md)을 따른다.
Google 조건 검색 → 최대 20개 후보 → 자체 점수 정렬 → 상위 최대 5개가 목표다.
현재 Google 후보 조회 클라이언트까지 구현했으며, 실제 호출 검증과 최종 추천 API는 아직 남아 있다.

## 실행

로컬 PostgreSQL의 `pick_eat` DB를 준비한 뒤 실행한다.
연결 설정은 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 환경변수로 지정할 수 있다.
비밀번호를 저장소에 커밋하지 않는다.

```sh
./gradlew bootRun --args='--spring.profiles.active=local'
```

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

IntelliJ 실행 구성에서는 Active profiles에 `local`을 지정한다.
문서는 `local`에서만 활성화한다. 배포 서버에는 `local`을 지정하지 않는다.
업무 API는 아직 없으며, 문서 공개와 무관하게 인증은 유지한다.
현재 기본 인증은 개발용이며 실제 사용자 인증 구현을 대체하지 않는다.

## 테스트

Docker Desktop을 실행한 뒤 다음 명령을 사용한다.

```sh
./gradlew build
```

Spring 통합 테스트는 `@Import(TestDatabaseConfig.class)`로 임시 PostgreSQL 18 / PostGIS 3.6에
연결한다. Flyway도 임시 DB에 적용한다. Docker가 없으면 테스트는 실패하며
개발 DB로 대체하거나 테스트를 건너뛰지 않는다. 첫 실행에는 이미지 다운로드가 필요하다.
새 DB 통합 테스트에도 이 설정을 적용해야 한다. DB 없는 단위 테스트에는 필요 없다.
Gradle 테스트에는 접속 불가능한 기본 DB 주소를 지정하여 컨테이너 설정 누락 시 실패하게 한다.
IntelliJ에서도 테스트 실행 도구를 Gradle로 설정한다.
테스트 이미지는 `postgis/postgis:18-3.6`이다. amd64 이미지이므로 Apple Silicon에서는
Docker의 amd64 에뮬레이션이 필요하며 실행이 더 느릴 수 있다.

TDD: 실패하는 테스트 작성 → 최소 구현 → 테스트를 유지하며 리팩토링.

## CI 및 리뷰

- `main` / `develop` 대상 PR 및 해당 브랜치 push에 빌드·테스트 실행.
- GitHub-hosted Ubuntu의 Docker를 사용하므로 개발 DB 접속 정보는 필요 없다.
- `.coderabbit.yaml`은 한국어 리뷰 기준이다. CodeRabbit GitHub App을 이 저장소에
  연결하고 이용 가능한 요금제를 확인해야 실제 PR 리뷰가 실행된다.
- 설정 파일을 GitHub에 올리기 전에는 원격 CI와 리뷰가 활성화되지 않는다.
- CD 및 Prometheus/Grafana 수집·대시보드·알림은 아직 구성하지 않았다.

## DB 변경

`src/main/resources/db/migration`의 버전별 SQL로 관리한다.
적용된 마이그레이션은 수정하지 않고 다음 버전 파일을 추가한다.

V2는 PostGIS 확장을 활성화하고 동행 적합도 5개(nullable boolean),
`location geography(Point, 4326)` 및 GiST 인덱스를 추가한다.
DB 서버에 PostGIS 패키지가 설치되어 있어야 한다. 확장 설치 권한이 없는 환경에서는
관리자가 해당 DB에 PostGIS를 먼저 활성화한다. 앱 계정에 관리자 권한을 부여하지 않는다.
`location`은 위도·경도로부터 자동 생성되므로 직접 입력하거나 수정하지 않는다.
좌표 입력 순서는 애플리케이션 필드와 무관하게 PostGIS Point에서 경도, 위도다.
반경 검색은 `ST_DWithin(location, 기준위치::geography, 5000)`을 사용한다.
V2 자체는 추천 순위 계산이나 API를 구현하지 않는다.

## 식당 저장·조회

- `domain.restaurant.entity.Restaurant`와 `RestaurantRepository`로 V1·V2 테이블에 접근한다.
- 생성은 `Restaurant.builder()`를 사용하며 ID와 생성·수정 시각은 직접 지정하지 않는다.
- 동행 적합도는 `Boolean`으로 미확인(`null`)과 부적합(`false`)을 구분한다.
- `location`은 Entity에 매핑하지 않는다. 위도·경도 저장 시 DB에서 자동 생성한다.
- `RestaurantRepositoryTest`는 임시 DB에서 저장 후 재조회, 미확인 정보, 동행 값,
  자동 생성 위치, 없는 ID 조회와 좌표 범위 제약을 검증한다.
- 반경 조회 API, 데이터 등록 API 및 추천 순위는 아직 구현하지 않았다.

## Google Places 후보 조회

- `GooglePlacesClient.findNearbyRestaurants`는 좌표와 Google 정렬 기준(`POPULARITY` 또는 `DISTANCE`)을 받는다.
- 식당 타입, 반경 5,000m, 최대 20개로 조회한다. 미정인 서비스 음식 분류 매핑은 아직 적용하지 않았다.
- 후보를 5개로 자르거나 DB에 저장하지 않는다. 자체 점수 계산은 다음 단계다.
- `GOOGLE_PLACES_API_KEY` 환경변수가 필요하다. 키 없이도 앱/테스트는 시작되지만 Google 조회는 실패한다.
- IntelliJ 실행 구성의 환경변수에 키를 지정한다. `.env` 파일은 자동으로 읽지 않는다.
- 평점·평가 수를 요청하므로 Nearby Search Enterprise 과금 대상이다. 실제 호출 전 API 사용 설정,
  키의 API/서버 제한, 할당량과 예산 알림을 확인한다. 예산 알림은 지출 차단 장치가 아니다.
- 연결 3초/응답 5초 제한, 자동 재시도 없음. 오류를 빈 목록이나 로컬 DB 결과로 바꾸지 않는다.
- 응답의 Google 지도 링크와 제공자 출처를 보존한다. 프런트 공개 전 Google 표시·귀속 정책을 적용해야 한다.
- 아직 HTTP 엔드포인트로 공개하지 않았다. Swagger에는 기존 설정만 유지한다.

Google 호출 없이 단위 테스트만 실행:

```sh
./gradlew test --tests '*GooglePlacesClientTest'
```

공식 규격: [Nearby Search](https://developers.google.com/maps/documentation/places/web-service/nearby-search),
[표시 및 저장 정책](https://developers.google.com/maps/documentation/places/web-service/policies).

## 코드 구조 분석

Graphify는 로컬 코드 분석 결과를 `graphify-out/`에 생성하며 Git에는 포함하지 않는다.
2026-09-15 분석은 Java 중심이고 SQL 파서 부재로 마이그레이션 내부 관계는 포함되지 않았다.
Ponytail 원칙에 따라 별도 Google SDK나 단일 구현용 인터페이스 없이 Spring RestClient를 사용한다.
