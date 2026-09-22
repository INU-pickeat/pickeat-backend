ALTER TABLE picks
    ADD COLUMN companion_type VARCHAR(20);

UPDATE picks p
SET companion_type = rs.companion_type
FROM recommendation_sessions rs
WHERE p.recommendation_session_id = rs.id
  AND p.companion_type IS NULL;

ALTER TABLE picks
    ALTER COLUMN companion_type SET NOT NULL;

ALTER TABLE picks
    ADD CONSTRAINT picks_companion_type
        CHECK (companion_type IN ('DATE', 'FAMILY', 'CHILDREN', 'SOLO', 'GROUP', 'DOG'));

COMMENT ON COLUMN picks.companion_type IS 'Pick 생성 시점의 추천 세션 동행 유형 스냅샷. 이후 세션이 바뀌어도 변하지 않는다';
