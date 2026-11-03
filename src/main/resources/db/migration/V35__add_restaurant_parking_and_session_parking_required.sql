-- 추천 조건 "주차 가능". 기존 식당은 NULL(모름)로 두고 다음 Google 갱신 때 채운다.
-- 기존 세션은 주차 조건 없이 만든 것이므로 false로 채운다.
ALTER TABLE restaurants
    ADD COLUMN parking_available BOOLEAN;

ALTER TABLE recommendation_sessions
    ADD COLUMN parking_required BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN restaurants.parking_available IS 'Google parkingOptions 기준 주차 가능 여부. 하나라도 true면 true, 받은 값이 모두 false면 false, NULL이면 모름';
COMMENT ON COLUMN recommendation_sessions.parking_required IS '추천 요청의 주차 가능 조건. true면 주차 불가(false) 식당을 빼고 주차 가능 식당을 앞에 둔다';
