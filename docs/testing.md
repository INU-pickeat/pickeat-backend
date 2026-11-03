# Testing

테스트 종류와 네이밍 규칙이다. 실행 방법은 [README.md](../README.md#테스트)를 따른다.

## 종류와 위치

| 종류 | 클래스명 | 위치 | 용도 |
| --- | --- | --- | --- |
| 단위 | `{대상 클래스}Test` | 대상과 같은 패키지 | Entity, Service(저장소는 목), 점수 계산 등 |
| API 계약 | `{도메인}ApiContractTest` | `domain/{도메인}/controller` | 요청 검증, 상태 코드, 응답 형식, 인증 여부 |
| 통합 | `{기능}IntegrationTest` | `com.pickeat.pickeatbackend` 루트 | 실제 DB로 쿼리·마이그레이션·제약 조건 검증 |

- 통합 테스트는 `@SpringBootTest` + `@Transactional`로 각 테스트를 롤백한다. 로컬 `localhost:5432` PostgreSQL(PostGIS)을 쓰며 CI도 같은 구성이다
- 로컬 DB에 다른 데이터가 있을 수 있으므로 건수는 넣기 전후 차이로 검증하거나, 테스트에서 만든 회원·식당으로 범위를 좁힌다
- 목으로 대체한 Service 테스트는 JPQL·SQL 자체를 검증하지 못한다. 새 쿼리는 통합 테스트를 하나 둔다

## 메서드 네이밍

- 테스트 메서드명은 영어 camelCase로 동작과 결과를 드러낸다 (`rejectsSecondReviewForPick`)
- 한국어 설명은 `@DisplayName`에 쓴다. 한글 메서드명은 쓰지 않는다

```java
@Test
@DisplayName("Pick 하나에는 후기를 하나만 저장할 수 있다")
void rejectsSecondReviewForPick() {
}
```
