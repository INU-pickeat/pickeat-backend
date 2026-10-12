-- 사용자가 고른 추천 검색 반경(m). 수도권·부산 500·1000·2000, 그 외 1000·3000·5000.
-- 기존 세션은 NULL로 두고 조회할 때 지역 기본값(수도권·부산 1000, 그 외 5000)으로 본다.
ALTER TABLE recommendation_sessions
    ADD COLUMN radius_meters INTEGER,
    ADD CONSTRAINT recommendation_sessions_radius_meters_check
        CHECK (radius_meters IS NULL OR radius_meters IN (500, 1000, 2000, 3000, 5000));

COMMENT ON COLUMN recommendation_sessions.radius_meters IS '추천 검색 반경(m). NULL이면 지역 기본값';
