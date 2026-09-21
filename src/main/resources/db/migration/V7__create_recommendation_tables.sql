CREATE TABLE recommendation_sessions (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id       BIGINT NOT NULL REFERENCES members (id),
    companion_type  VARCHAR(20) NOT NULL,
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT recommendation_sessions_companion_type
        CHECK (companion_type IN ('DATE', 'FRIENDS', 'FAMILY', 'SOLO', 'GROUP_DINNER')),
    CONSTRAINT recommendation_sessions_latitude_range CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT recommendation_sessions_longitude_range CHECK (longitude BETWEEN -180 AND 180)
);

CREATE TABLE recommendation_session_food_categories (
    session_id     BIGINT NOT NULL REFERENCES recommendation_sessions (id) ON DELETE CASCADE,
    food_category  VARCHAR(30) NOT NULL,

    PRIMARY KEY (session_id, food_category),
    CONSTRAINT recommendation_session_food_categories_category
        CHECK (food_category IN ('KOREAN', 'JAPANESE', 'CHINESE', 'WESTERN', 'CAFE_DESSERT'))
);

CREATE TABLE recommendation_candidates (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id             BIGINT NOT NULL REFERENCES recommendation_sessions (id) ON DELETE CASCADE,
    restaurant_id          BIGINT NOT NULL REFERENCES restaurants (id),
    result_rank            INT NOT NULL,
    distance_meters        DOUBLE PRECISION NOT NULL,
    rating_contribution    DOUBLE PRECISION NOT NULL,
    distance_contribution  DOUBLE PRECISION NOT NULL,
    companion_bonus        DOUBLE PRECISION NOT NULL,
    total_score            DOUBLE PRECISION NOT NULL,

    CONSTRAINT recommendation_candidates_rank_range CHECK (result_rank BETWEEN 1 AND 5),
    CONSTRAINT recommendation_candidates_distance_range CHECK (distance_meters BETWEEN 0 AND 5000),
    CONSTRAINT recommendation_candidates_session_rank_unique UNIQUE (session_id, result_rank),
    CONSTRAINT recommendation_candidates_session_restaurant_unique UNIQUE (session_id, restaurant_id)
);

CREATE INDEX recommendation_sessions_member_id_idx ON recommendation_sessions (member_id);
CREATE INDEX recommendation_candidates_restaurant_id_idx ON recommendation_candidates (restaurant_id);

COMMENT ON TABLE recommendation_sessions IS '한 번의 추천 요청 조건(위치·동행 유형)을 기록';
COMMENT ON TABLE recommendation_session_food_categories IS '추천 요청에 포함된 음식 카테고리 목록(다대다)';
COMMENT ON TABLE recommendation_candidates IS 'ADR-M2-1 점수 구성 요소를 포함한 세션별 추천 결과(Top 5)';
COMMENT ON COLUMN recommendation_candidates.result_rank IS '결과 내 순위(1~5)';
COMMENT ON COLUMN recommendation_candidates.rating_contribution IS 'w1 * (externalRating / 5)';
COMMENT ON COLUMN recommendation_candidates.distance_contribution IS 'w2 * (1 - distanceMeters / 5000)';
COMMENT ON COLUMN recommendation_candidates.companion_bonus IS '동행 적합 일치 시 가산점, 불일치 시 0';
COMMENT ON COLUMN recommendation_candidates.total_score IS 'rating_contribution + distance_contribution + companion_bonus';
