-- 탐색 스팟 식당 목록 정정 (2026-10-08 전달 목록 기준).
-- 사진 파일(/images/discovery/{region}_{NN}_main.jpg)은 원래 맞게 들어가 있었고, V14 시드의 식당·순서가 사진과 어긋나 있었다.
-- 혜화: 순대실록·부부식당 → 정돈·아란치아, 학림다방 3위
-- 한남: 오만지아·이태원우육미옌 → 바다식당·바바라스키친, 쥬에 1위·타크 2위, 쥬에 소개 문구 수정
-- 종로: 서린낙지 → 대장장이화덕피자
-- 신사·서촌은 그대로다. Place ID·좌표·평점은 2026-10-08 Places Text Search로 확인한 값.

INSERT INTO restaurants (
    data_provider,
    google_place_id,
    name,
    food_category,
    address,
    latitude,
    longitude,
    phone_number,
    opening_hours_text,
    external_rating,
    external_rating_count,
    external_data_refreshed_at
)
VALUES
    ('CURATED', 'ChIJ1fcl6CujfDURIfH0uohi-dM', '정돈', 'JAPANESE', '서울 종로구 대학로9길 12 지하1층', 37.581773, 127.00108370000001, '02-987-0924', '매일 11:30~21:30', 4.4, 2054, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJ7YfRu4ijfDURBEakpvnFbVc', '아란치아', 'WESTERN', '서울 종로구 대학로 81-1 1-2층', 37.578502799999995, 127.00191699999999, '02-762-6334', '매일 11:00~21:00', 5, 21, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJWYCFjDGjfDUR-yif2yI6ykw', '바다식당', 'KOREAN', '서울 용산구 이태원로 245 2층', 37.536850199999996, 127.00013489999999, '02-795-1317', '매일 11:30~22:00', 3.7, 628, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJiU44bnCjfDURH7W8POMWWss', '바바라스키친', 'KOREAN', '서울 용산구 대사관로5길 17 1층', 37.5349764, 127.0005666, NULL, '화~일 11:30~21:00 (14:20~17:00 브레이크타임)', 4.2, 101, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJ4Q1Ca8WifDURRr2G9wHjgHc', '대장장이화덕피자', 'WESTERN', '서울 종로구 북촌로 42-4', 37.5805576, 126.98535769999998, '02-765-4298', '월, 수~일 11:30~21:30 (화요일 휴무)', 4.4, 501, CURRENT_TIMESTAMP)
ON CONFLICT (google_place_id) WHERE google_place_id IS NOT NULL DO UPDATE SET
    data_provider = EXCLUDED.data_provider,
    name = EXCLUDED.name,
    food_category = EXCLUDED.food_category,
    secondary_food_category = NULL,
    address = EXCLUDED.address,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    phone_number = COALESCE(EXCLUDED.phone_number, restaurants.phone_number),
    opening_hours_text = EXCLUDED.opening_hours_text,
    external_rating = EXCLUDED.external_rating,
    external_rating_count = EXCLUDED.external_rating_count,
    external_data_refreshed_at = EXCLUDED.external_data_refreshed_at,
    updated_at = CURRENT_TIMESTAMP;

-- 목록에서 빠지는 5곳은 일반 Google 식당으로 돌린다. 잘못 연결된 탐색 사진도 지워
-- 다른 Google 식당처럼 식당 사진 API(/api/v1/restaurants/{id}/photo)를 쓰게 한다.
UPDATE restaurants
SET data_provider = 'GOOGLE',
    representative_image_url = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE google_place_id IN (
    'ChIJe1KSjyyjfDURjZzF2YW58Q4', -- 순대실록 대학로본점
    'ChIJiyaPJiyjfDURGm2g5RjBL-s', -- 부부식당
    'ChIJNV_UWbqjfDUR0S4kyPrEI14', -- 오만지아
    'ChIJ5fdGqrOjfDURkKXkH1T8W8E', -- 이태원우육미옌
    'ChIJoZkzduyifDURqPk0qC1EYT4'  -- 서린낙지
);

-- 바뀐 세 지역의 노출 목록을 새로 채운다.
DELETE FROM discovery_spot_restaurants
WHERE discovery_spot_id IN (
    SELECT id FROM discovery_spots WHERE region_code IN ('HYEHWA', 'HANNAM', 'JONGNO')
);

INSERT INTO discovery_spot_restaurants (
    discovery_spot_id,
    restaurant_id,
    display_order,
    one_line_intro
)
SELECT
    spot.id,
    restaurant.id,
    seed.display_order,
    seed.one_line_intro
FROM (VALUES
    ('HYEHWA', 'ChIJxbEiNKijfDURuvS-mWItac0', 1, '예약 필수 감성 가득한 이탈리안 식당'),
    ('HYEHWA', 'ChIJmaJ4HES9fDURewNUBzhTTJg', 2, '45년 전통 깊은 사골 칼국수'),
    ('HYEHWA', 'ChIJzZCu7SujfDURGD9aedZkmYM', 3, '분위기 좋은 오래된 앤틱한 감성 카페'),
    ('HYEHWA', 'ChIJ1fcl6CujfDURIfH0uohi-dM', 4, '한국에서 일식 돈카츠 유행의 시초'),
    ('HYEHWA', 'ChIJ7YfRu4ijfDURBEakpvnFbVc', 5, '화덕에서 구워낸 쫄깃한 도우의 맛있는 피자'),
    ('HANNAM', 'ChIJb8Vo4EqjfDURM0hmpQh5Mtg', 1, '예술적인 공간에서 맛있는 광둥식 중식'),
    ('HANNAM', 'ChIJ8TKznFCjfDURX6tGaNwVp9E', 2, '한남동 골목 안의 맛있는 멕시칸 타코'),
    ('HANNAM', 'ChIJWYCFjDGjfDUR-yif2yI6ykw', 3, '사골육수 기반의 맛있는 존슨탕'),
    ('HANNAM', 'ChIJiU44bnCjfDURH7W8POMWWss', 4, '모던함과 퓨전이 섞인 분식집'),
    ('HANNAM', 'ChIJFagRhtGjfDURra0NGYpBM8w', 5, '한남동 거리의 정성 가득한 커피'),
    ('JONGNO', 'ChIJ2WFkCumifDURLU2wDQbqtLU', 1, '진한 닭한마리와 맛있는 떡사리의 조화'),
    ('JONGNO', 'ChIJJyemnySjfDURH7QCM91Wq_8', 2, '마늘 가득한 향의 닭도리탕'),
    ('JONGNO', 'ChIJc9BLad2ifDURS4ivHkCVpNY', 3, '세운상가 지하의 정겹고 맛있는 백반집'),
    ('JONGNO', 'ChIJ4Q1Ca8WifDURRr2G9wHjgHc', 4, '한옥에서 먹는 정통 나폴리 화덕피자'),
    ('JONGNO', 'ChIJe38rcMKjfDUREvbBk6fhcxE', 5, '종로구의 맛있는 일식 돈카츠집')
) AS seed(region_code, google_place_id, display_order, one_line_intro)
JOIN discovery_spots spot ON spot.region_code = seed.region_code
JOIN restaurants restaurant ON restaurant.google_place_id = seed.google_place_id;

-- 순번이 바뀌었으니 사진 경로도 V15와 같은 규칙으로 다시 연결한다.
UPDATE restaurants restaurant
SET representative_image_url = '/images/discovery/'
    || LOWER(spot.region_code)
    || '_'
    || LPAD(spot_restaurant.display_order::TEXT, 2, '0')
    || '_main.jpg'
FROM discovery_spot_restaurants spot_restaurant
JOIN discovery_spots spot ON spot.id = spot_restaurant.discovery_spot_id
WHERE restaurant.id = spot_restaurant.restaurant_id;
