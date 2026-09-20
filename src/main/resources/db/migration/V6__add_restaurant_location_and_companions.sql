CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE restaurants
    ADD COLUMN IF NOT EXISTS suitable_for_date BOOLEAN,
    ADD COLUMN IF NOT EXISTS suitable_for_friends BOOLEAN,
    ADD COLUMN IF NOT EXISTS suitable_for_family BOOLEAN,
    ADD COLUMN IF NOT EXISTS suitable_for_solo BOOLEAN,
    ADD COLUMN IF NOT EXISTS suitable_for_group_dinner BOOLEAN,
    ADD COLUMN IF NOT EXISTS location geography(Point, 4326)
        GENERATED ALWAYS AS (
            ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
        ) STORED NOT NULL;

CREATE INDEX IF NOT EXISTS restaurants_location_gist_idx ON restaurants USING GIST (location);

COMMENT ON COLUMN restaurants.location IS '위도·경도에서 생성되는 WGS84 위치. 거리 단위는 미터';
COMMENT ON COLUMN restaurants.suitable_for_date IS '데이트 적합도: true=적합, false=부적합, NULL=미확인';
COMMENT ON COLUMN restaurants.suitable_for_friends IS '친구 동행 적합도: true=적합, false=부적합, NULL=미확인';
COMMENT ON COLUMN restaurants.suitable_for_family IS '가족 동행 적합도: true=적합, false=부적합, NULL=미확인';
COMMENT ON COLUMN restaurants.suitable_for_solo IS '혼밥 적합도: true=적합, false=부적합, NULL=미확인';
COMMENT ON COLUMN restaurants.suitable_for_group_dinner IS '회식 적합도: true=적합, false=부적합, NULL=미확인';
