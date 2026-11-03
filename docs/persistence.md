# Persistence

DB 네이밍, 엔티티, 시간 타입, 트랜잭션, 마이그레이션 규칙이다. ERD는 Notion BE 페이지에 있다.

## DB 네이밍

- 테이블명은 snake_case 복수형(`picks`, `recommendation_sessions`), 컬럼명은 snake_case다
- 제약 조건과 인덱스는 `{테이블}_{내용}` 형식으로 이름을 붙인다 (`picks_status`, `picks_member_selected_idx`)

## 엔티티

- 도메인 명사를 접미사 없이 쓴다 (`Pick` ○ / `PickEntity` ✕)
- `@Getter`와 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`를 기본으로 붙인다
- 생성에는 생성자에 붙인 `@Builder`를 써도 된다. `@Data`, `@Setter`는 쓰지 않는다
- 값 변경은 `updateProfile`, `changePassword`처럼 행동을 드러내는 메서드로 한다
- ID는 `Long`, DB는 `IDENTITY` 컬럼이다
- Enum은 `@Enumerated(EnumType.STRING)`으로 저장하고, DB에도 같은 값의 CHECK 제약을 건다

## 시간

- 시각은 `Instant`, DB 컬럼은 `TIMESTAMPTZ`를 쓴다. `LocalDateTime`과 시간대 없는 `TIMESTAMP`는 쓰지 않는다
- 날짜 경계(오늘, 이번 주, 캘린더 월)는 `ZoneId.of("Asia/Seoul")` 기준으로 계산한다
- 생성·수정 시각은 DB 기본값(`DEFAULT CURRENT_TIMESTAMP`)이나 엔티티의 `@PrePersist`/`@PreUpdate`로 채운다

## 연관관계

- 실제로 필요한 방향만 매핑한다. 기본은 자식 → 부모 단방향 `@ManyToOne(fetch = FetchType.LAZY)`다
- `@OneToMany`는 부모와 생명주기를 함께하는 자식(후기 이미지처럼 `cascade`·`orphanRemoval`이 필요한 경우)에만 쓴다
- 목록·집계는 Repository 쿼리로 조회한다. 여러 테이블을 묶는 집계는 네이티브 SQL(`JdbcTemplate` 또는 `@Query(nativeQuery = true)`)을 써도 된다

## 트랜잭션

- 데이터를 바꾸는 Service 메서드에 `@Transactional`을 붙인다
- 조회 메서드에는 `@Transactional(readOnly = true)`를 붙인다. 조회만 하는 Service는 클래스에 붙여도 된다

## 마이그레이션

- DB 변경은 Flyway로만 한다. `ddl-auto`는 `validate`다
- 파일은 `src/main/resources/db/migration/V{번호}__{영문_snake_case_설명}.sql`이고 번호는 마지막 번호 다음이다
- 이미 실행된 마이그레이션 파일은 고치지 않는다. 잘못됐으면 새 버전으로 바로잡는다
- Entity 변경과 마이그레이션 SQL은 같은 PR에 넣는다
- 새 테이블·컬럼에는 `COMMENT ON`으로 한국어 설명을 남기고, 값 범위가 정해진 컬럼에는 CHECK 제약을 건다
- 기존 데이터를 변환하는 마이그레이션은 변환 기준(시간대, 기본값 등)을 SQL 주석에 적는다
