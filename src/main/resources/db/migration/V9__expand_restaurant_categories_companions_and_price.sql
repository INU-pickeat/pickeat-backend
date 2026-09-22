ALTER TABLE restaurants
    DROP CONSTRAINT IF EXISTS restaurants_food_category;

ALTER TABLE restaurants
    ADD CONSTRAINT restaurants_food_category
        CHECK (food_category IS NULL OR food_category IN (
            'KOREAN', 'JAPANESE', 'CHINESE', 'WESTERN', 'CAFE_DESSERT', 'PUB_BAR', 'OTHER'
        ));

ALTER TABLE recommendation_session_food_categories
    DROP CONSTRAINT IF EXISTS recommendation_session_food_categories_category;

ALTER TABLE recommendation_session_food_categories
    ADD CONSTRAINT recommendation_session_food_categories_category
        CHECK (food_category IN (
            'KOREAN', 'JAPANESE', 'CHINESE', 'WESTERN', 'CAFE_DESSERT', 'PUB_BAR', 'OTHER'
        ));

ALTER TABLE recommendation_sessions
    DROP CONSTRAINT IF EXISTS recommendation_sessions_companion_type;

UPDATE recommendation_sessions
SET companion_type = 'GROUP'
WHERE companion_type IN ('FRIENDS', 'GROUP_DINNER');

ALTER TABLE recommendation_sessions
    ADD CONSTRAINT recommendation_sessions_companion_type
        CHECK (companion_type IN ('DATE', 'FAMILY', 'CHILDREN', 'SOLO', 'GROUP', 'DOG'));

ALTER TABLE restaurants
    ADD COLUMN IF NOT EXISTS price_range_start NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS price_range_end NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS price_currency_code VARCHAR(3),
    ADD COLUMN IF NOT EXISTS suitable_for_children BOOLEAN,
    ADD COLUMN IF NOT EXISTS suitable_for_group BOOLEAN,
    ADD COLUMN IF NOT EXISTS suitable_for_dogs BOOLEAN;

UPDATE restaurants
SET suitable_for_group = suitable_for_group_dinner
WHERE suitable_for_group IS NULL;

ALTER TABLE restaurants
    DROP COLUMN IF EXISTS suitable_for_friends,
    DROP COLUMN IF EXISTS suitable_for_group_dinner;

ALTER TABLE restaurants
    ADD CONSTRAINT restaurants_price_range_non_negative
        CHECK (
            (price_range_start IS NULL OR price_range_start >= 0)
            AND (price_range_end IS NULL OR price_range_end >= 0)
        ),
    ADD CONSTRAINT restaurants_price_range_order
        CHECK (
            price_range_start IS NULL
            OR price_range_end IS NULL
            OR price_range_start <= price_range_end
        ),
    ADD CONSTRAINT restaurants_price_currency_code_format
        CHECK (
            price_currency_code IS NULL
            OR price_currency_code ~ '^[A-Z]{3}$'
        );

COMMENT ON COLUMN restaurants.price_range_start IS 'Google Places 1인당 가격 범위 하한';
COMMENT ON COLUMN restaurants.price_range_end IS 'Google Places 1인당 가격 범위 상한. NULL이면 상한 미제공';
COMMENT ON COLUMN restaurants.price_currency_code IS 'Google Places 가격의 ISO 4217 통화 코드';
COMMENT ON COLUMN restaurants.suitable_for_children IS '아이 동반 적합도: goodForChildren OR menuForChildren';
COMMENT ON COLUMN restaurants.suitable_for_group IS '단체 적합도: goodForGroups';
COMMENT ON COLUMN restaurants.suitable_for_dogs IS '반려견 동반 적합도: allowsDogs';
