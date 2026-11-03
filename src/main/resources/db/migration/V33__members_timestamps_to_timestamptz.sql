-- members만 시간대 없는 TIMESTAMP였다. 다른 테이블처럼 TIMESTAMPTZ로 맞추고 엔티티는 Instant를 쓴다.
-- 기존 값은 애플리케이션 JVM 기본 시간대(운영 EC2 Ubuntu 기본값 UTC)의 LocalDateTime으로 저장됐으므로 UTC로 해석한다.
ALTER TABLE members
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';
