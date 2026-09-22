ALTER TABLE recommendation_sessions
    ADD COLUMN IF NOT EXISTS price_range_min NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS price_range_max NUMERIC(19, 2);

ALTER TABLE recommendation_sessions
    ADD CONSTRAINT recommendation_sessions_price_range_non_negative
        CHECK (
            (price_range_min IS NULL OR price_range_min >= 0)
            AND (price_range_max IS NULL OR price_range_max >= 0)
        ),
    ADD CONSTRAINT recommendation_sessions_price_range_order
        CHECK (
            price_range_min IS NULL
            OR price_range_max IS NULL
            OR price_range_min <= price_range_max
        );

ALTER TABLE recommendation_candidates
    DROP CONSTRAINT IF EXISTS recommendation_candidates_rank_range;

ALTER TABLE recommendation_candidates
    ADD CONSTRAINT recommendation_candidates_rank_range CHECK (result_rank BETWEEN 1 AND 10);

COMMENT ON COLUMN recommendation_sessions.price_range_min IS '요청 당시 가격 필터 하한. NULL이면 가격 무관';
COMMENT ON COLUMN recommendation_sessions.price_range_max IS '요청 당시 가격 필터 상한. NULL이면 가격 무관';
COMMENT ON COLUMN recommendation_candidates.result_rank IS '결과 내 순위(1~10). 1~5는 응답 노출, 6~10은 제외 시 대체 후보';
