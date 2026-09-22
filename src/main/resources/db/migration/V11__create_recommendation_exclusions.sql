CREATE TABLE recommendation_exclusions (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id    BIGINT NOT NULL REFERENCES recommendation_sessions (id) ON DELETE CASCADE,
    restaurant_id BIGINT NOT NULL REFERENCES restaurants (id),
    reason        VARCHAR(30) NOT NULL,
    excluded_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT recommendation_exclusions_reason
        CHECK (reason IN (
            'DISTANCE_TOO_FAR', 'PRICE_TOO_HIGH', 'MENU_UNSATISFACTORY', 'ATMOSPHERE_MISMATCH', 'WANT_DIFFERENT'
        )),
    CONSTRAINT recommendation_exclusions_session_restaurant_unique UNIQUE (session_id, restaurant_id)
);

CREATE INDEX recommendation_exclusions_session_id_idx ON recommendation_exclusions (session_id);

COMMENT ON TABLE recommendation_exclusions IS '세션 내에서 제외된 식당과 사유. 해당 세션 안에서만 유효하고 다른 세션·이후 추천에는 영향을 주지 않는다(영구 차단 아님)';
COMMENT ON COLUMN recommendation_exclusions.reason IS '제외 사유. 추천 알고리즘 개선용 데이터로 활용한다';
