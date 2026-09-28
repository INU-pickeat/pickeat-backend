-- 운영자 선정 초기 탐색 스팟 25곳.
-- 이름·카테고리·소개·영업시간은 2026-09-23 확정 기획 데이터를 기준으로 하고,
-- Google Place ID·좌표·외부 평점은 Places Text Search로 검증한 값만 저장한다.
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
    ('CURATED', 'ChIJa3TjemujfDURngJToIQ6D8o', '야스노야지로 압구정점', 'JAPANESE', '서울 강남구 논현로163길 13-5 한가빌딩 1층', 37.523692499999996, 127.02705290000002, '02-515-0818', '매일 17:00~22:00', 4.3, 88, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJB5illI-jfDUR_wQre3BcvQM', '산동교자관', 'CHINESE', '서울 강남구 압구정로 214', 37.527954, 127.03071969999999, '02-514-2608', '월~토 12:00~21:00 (일요일 휴무)', 4, 204, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJ9SDzMY6jfDURTT_YbqA7pOs', '가담', 'CHINESE', '서울 강남구 언주로167길 35 1층', 37.5263754, 127.0302672, '02-545-5163', '매일 11:20~21:30 (15:00~17:00 브레이크타임)', 4.3, 680, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJozsev8KjfDURjiTMXj1HkU8', '영동설렁탕', 'KOREAN', '서울 서초구 강남대로101안길 24 1층', 37.5161224, 127.0174252, '02-543-4716', '매일 24시간', 4.1, 3269, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJ55AmZ6ajfDURjzmQUSRRke4', '멘쇼쿠', 'JAPANESE', '서울 서초구 강남대로99길 10 B동 2층', 37.515295, 127.01881750000001, '010-5545-7226', '매일 11:30~20:30', 4.8, 795, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJxbEiNKijfDURuvS-mWItac0', '오쏘파스타', 'WESTERN', '서울 종로구 낙산길 21 1층', 37.5805436, 127.00572009999998, '010-2817-9935', '토~일 12:20~19:50', 4.5, 74, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJmaJ4HES9fDURewNUBzhTTJg', '혜화칼국수', 'KOREAN', '서울 종로구 창경궁로35길 13 1층', 37.5865636, 127.00151559999999, '02-743-8212', '매일 11:00~21:00 (15:00~16:00 브레이크타임)', 4, 486, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJe1KSjyyjfDURjZzF2YW58Q4', '순대실록 대학로본점', 'KOREAN', '서울 종로구 동숭길 113 1층', 37.5829777, 127.00365839999999, '02-742-5338', '매일 10:00~23:30', 4.5, 2270, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJzZCu7SujfDURGD9aedZkmYM', '학림다방', 'CAFE_DESSERT', '서울 종로구 대학로 119 2층', 37.5818924, 127.0016664, '02-742-2877', '매일 10:00~22:50', 4.4, 1112, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJiyaPJiyjfDURGm2g5RjBL-s', '부부식당', 'KOREAN', '서울 종로구 동숭길 43 2층', 37.580279499999996, 127.00430039999999, '02-765-6056', '월~토 11:00~21:30 (일요일 휴무)', 3.9, 94, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJV68ntpGjfDURPMKwivoxFPA', '도량', 'CHINESE', '서울 종로구 자하문로6길 6 백송빌딩 2층', 37.5779717, 126.97250659999999, '02-739-0145', '화~일 11:00~21:30 (14:30~17:00 브레이크타임)', 4.5, 111, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJDYTfdb6jfDURuyUAZqMD6h8', '팔', 'CAFE_DESSERT', '서울 종로구 자하문로9길 6 1층', 37.5795355, 126.97088740000001, '010-4814-2468', '매일 10:00~20:00', 4.3, 36, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJKySr91CjfDUR88Oj8ibBPvg', '서촌곳간', 'PUB_BAR', '서울 종로구 필운대로 6-1 아이에스빌딩 1층', 37.5767022, 126.96930060000001, '010-4098-0501', '매일 16:00~01:00', 4.6, 23, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJ4e1Hz7ujfDURwg6f_LV9lkY', '아키비스트', 'CAFE_DESSERT', '서울 종로구 효자로13길 52 1층', 37.5821697, 126.9726616, '02-738-1517', '매일 09:00~20:30', 4.5, 137, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJby-1ZJaifDURWlNNATQsgUo', '김진목삼', 'KOREAN', '서울 종로구 자하문로1길 56 1층', 37.5771154, 126.96949699999999, '02-929-2929', '매일 11:30~22:00', 4.5, 332, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJ8TKznFCjfDURX6tGaNwVp9E', '타크', 'OTHER', '서울 용산구 이태원로42길 28-4', 37.5358871, 127.00040740000001, NULL, '매일 12:00~22:00', 4, 46, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJb8Vo4EqjfDURM0hmpQh5Mtg', '쥬에', 'CHINESE', '서울 용산구 독서당로 124-7 지하1층, 1-2층', 37.5374483, 127.01277890000001, '02-798-9700', '매일 11:30~21:30 (15:00~17:30 브레이크타임)', 4.4, 458, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJNV_UWbqjfDUR0S4kyPrEI14', '오만지아', 'WESTERN', '서울 용산구 유엔빌리지길 14 1층', 37.5339714, 127.00985550000001, '02-749-2900', '매일 12:00~24:00', 4.3, 434, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJ5fdGqrOjfDURkKXkH1T8W8E', '이태원우육미옌', 'CHINESE', '서울 용산구 이태원로55가길 26-8 1-2층', 37.5374652, 126.999893, '02-798-5556', '매일 11:00~21:00 (15:30~17:00 브레이크타임)', 4.1, 747, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJFagRhtGjfDURra0NGYpBM8w', '마일스톤커피 한남', 'CAFE_DESSERT', '서울 용산구 한남대로27가길 26 1층', 37.5378721, 127.00266519999998, '070-7807-3046', '매일 10:00~21:00', 4.4, 291, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJ2WFkCumifDURLU2wDQbqtLU', '백부장집닭한마리', 'KOREAN', '서울 종로구 삼봉로 100-1 1층', 37.5717819, 126.98267229999999, '02-732-2565', '월~토 11:00~22:00 (15:00~17:00 브레이크타임, 일요일 휴무)', 4.4, 163, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJJyemnySjfDURH7QCM91Wq_8', '종로계림닭도리탕 본점', 'KOREAN', '서울 종로구 돈화문로4길 39 1층', 37.570155899999996, 126.9945035, '02-2263-6658', '월~토 11:30~21:30 (15:30~16:30 브레이크타임, 일요일 휴무)', 4.1, 1740, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJc9BLad2ifDURS4ivHkCVpNY', '일미식당', 'KOREAN', '서울 종로구 삼일대로 428 낙원악기상가 지하1층 148호', 37.5728323, 126.9880012, '02-766-6588', '월~토 11:30~21:30 (15:30~16:30 브레이크타임, 일요일 휴무)', 4.1, 320, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJoZkzduyifDURqPk0qC1EYT4', '서린낙지', 'KOREAN', '서울 종로구 종로 19 르메이에르종로타운 2층', 37.5706284, 126.9801545, '02-735-0670', '월~토 11:30~21:30 (15:30~17:30 브레이크타임, 일요일 휴무)', 3.9, 1413, CURRENT_TIMESTAMP),
    ('CURATED', 'ChIJe38rcMKjfDUREvbBk6fhcxE', '일월카츠 계동점', 'JAPANESE', '서울 종로구 계동길 17 1층', 37.5781158, 126.98648349999999, '0503-7153-1966', '매일 11:30~21:00 (15:00~17:00 브레이크타임)', 4.8, 138, CURRENT_TIMESTAMP)
ON CONFLICT (google_place_id) WHERE google_place_id IS NOT NULL DO UPDATE SET
    data_provider = EXCLUDED.data_provider,
    name = EXCLUDED.name,
    food_category = EXCLUDED.food_category,
    address = EXCLUDED.address,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    phone_number = COALESCE(EXCLUDED.phone_number, restaurants.phone_number),
    opening_hours_text = EXCLUDED.opening_hours_text,
    external_rating = EXCLUDED.external_rating,
    external_rating_count = EXCLUDED.external_rating_count,
    external_data_refreshed_at = EXCLUDED.external_data_refreshed_at,
    updated_at = CURRENT_TIMESTAMP;

-- 같은 Google Place ID가 추천 과정에서 먼저 저장됐더라도 CURATED로 승격한다.
-- INSERT 대상에 없는 사진 URL과 수동 동행 적합도는 기존 값을 유지한다.

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
    ('SINSA', 'ChIJa3TjemujfDURngJToIQ6D8o', 1, '일본 감성 닮은 양고기 오마카세'),
    ('SINSA', 'ChIJB5illI-jfDUR_wQre3BcvQM', 2, '육즙 가득한 정통 중식 만두집'),
    ('SINSA', 'ChIJ9SDzMY6jfDURTT_YbqA7pOs', 3, '누룽지 품은 고추탕수육 명가'),
    ('SINSA', 'ChIJozsev8KjfDURjiTMXj1HkU8', 4, '맛있는 설렁탕과 수육'),
    ('SINSA', 'ChIJ55AmZ6ajfDURjzmQUSRRke4', 5, '진하고 쫄깃한 츠케멘'),
    ('HYEHWA', 'ChIJxbEiNKijfDURuvS-mWItac0', 1, '예약 필수 감성 가득한 이탈리안 식당'),
    ('HYEHWA', 'ChIJmaJ4HES9fDURewNUBzhTTJg', 2, '45년 전통 깊은 사골 칼국수'),
    ('HYEHWA', 'ChIJe1KSjyyjfDURjZzF2YW58Q4', 3, '전통과 연구가 빚어낸 순대 명작'),
    ('HYEHWA', 'ChIJzZCu7SujfDURGD9aedZkmYM', 4, '분위기 좋은 오래된 앤틱한 감성 카페'),
    ('HYEHWA', 'ChIJiyaPJiyjfDURGm2g5RjBL-s', 5, '감성 가득한 퓨전 분식 맛집'),
    ('SEOCHEON', 'ChIJV68ntpGjfDURPMKwivoxFPA', 1, '임태훈 셰프의 맛있는 중식 요리들'),
    ('SEOCHEON', 'ChIJDYTfdb6jfDURuyUAZqMD6h8', 2, '맛있는 커피와 달콤한 프렌치토스트'),
    ('SEOCHEON', 'ChIJKySr91CjfDUR88Oj8ibBPvg', 3, '감성 가득한 퓨전한식주점'),
    ('SEOCHEON', 'ChIJ4e1Hz7ujfDURwg6f_LV9lkY', 4, '쫀쫀하고 맛있는 아인슈페너 명가'),
    ('SEOCHEON', 'ChIJby-1ZJaifDURWlNNATQsgUo', 5, '직접 구워주는 맛있는 생삼겹살'),
    ('HANNAM', 'ChIJ8TKznFCjfDURX6tGaNwVp9E', 1, '한남동 골목 안의 맛있는 멕시칸 타코'),
    ('HANNAM', 'ChIJb8Vo4EqjfDURM0hmpQh5Mtg', 2, '예술적인 공간에서 맛있는 중식'),
    ('HANNAM', 'ChIJNV_UWbqjfDUR0S4kyPrEI14', 3, '한남동의 맛있는 이태리 식당'),
    ('HANNAM', 'ChIJ5fdGqrOjfDURkKXkH1T8W8E', 4, '빈티지 감성의 깊은 우육면'),
    ('HANNAM', 'ChIJFagRhtGjfDURra0NGYpBM8w', 5, '한남동 거리의 정성 가득한 커피'),
    ('JONGNO', 'ChIJ2WFkCumifDURLU2wDQbqtLU', 1, '진한 닭한마리와 맛있는 떡사리의 조화'),
    ('JONGNO', 'ChIJJyemnySjfDURH7QCM91Wq_8', 2, '마늘 가득한 향의 닭도리탕'),
    ('JONGNO', 'ChIJc9BLad2ifDURS4ivHkCVpNY', 3, '세운상가 지하의 정겹고 맛있는 백반집'),
    ('JONGNO', 'ChIJoZkzduyifDURqPk0qC1EYT4', 4, '화끈하고 맛있는 양념의 낙지볶음과 소세지볶음'),
    ('JONGNO', 'ChIJe38rcMKjfDUREvbBk6fhcxE', 5, '종로구의 맛있는 일식 돈카츠집')
) AS seed(region_code, google_place_id, display_order, one_line_intro)
JOIN discovery_spots spot ON spot.region_code = seed.region_code
JOIN restaurants restaurant ON restaurant.google_place_id = seed.google_place_id;

COMMENT ON TABLE discovery_spot_restaurants IS
    '탐색 지역별 운영자 선정 식당과 고정 노출 순서. 2026-09-23 최초 25곳 시드';
