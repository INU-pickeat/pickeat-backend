CREATE TABLE picks (
    id                         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id                  BIGINT NOT NULL REFERENCES members (id),
    restaurant_id              BIGINT NOT NULL REFERENCES restaurants (id),
    recommendation_session_id  BIGINT NOT NULL REFERENCES recommendation_sessions (id),
    status                     VARCHAR(20) NOT NULL DEFAULT 'SELECTED',
    selected_at                TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    visited_at                 TIMESTAMPTZ,
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT picks_status CHECK (status IN ('SELECTED', 'REVIEWED', 'CANCELED')),
    CONSTRAINT picks_recommendation_session_unique UNIQUE (recommendation_session_id)
);

CREATE INDEX picks_member_selected_idx ON picks (member_id, selected_at DESC, id DESC);
CREATE INDEX picks_restaurant_id_idx ON picks (restaurant_id);
CREATE INDEX picks_member_status_idx ON picks (member_id, status);

COMMENT ON TABLE picks IS '추천 결과에서 사용자가 최종 선택한 식당 기록';
COMMENT ON COLUMN picks.recommendation_session_id IS '추천 세션당 하나의 최종 Pick만 허용';
COMMENT ON COLUMN picks.visited_at IS 'REVIEWED 전환 시 기록되는 방문 완료 시각';
