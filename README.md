# Pick Eat Backend

Java 21 / Spring Boot 4.1.1 / PostgreSQL 기반 백엔드.

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
