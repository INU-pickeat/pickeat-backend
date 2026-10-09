-- 종로 탐색 스팟 4위 대장장이화덕피자의 최신 확정 정보 반영.
UPDATE restaurants
SET phone_number = '0507-1315-4298',
    opening_hours_text = '수~금 11:30~21:30 (15:00~17:00 브레이크타임, 20:40 라스트오더), 토~일 11:30~21:30 (15:00~16:30 브레이크타임, 20:40 라스트오더), 월~화 휴무',
    updated_at = CURRENT_TIMESTAMP
WHERE google_place_id = 'ChIJ4Q1Ca8WifDURRr2G9wHjgHc';

UPDATE discovery_spot_restaurants
SET one_line_intro = '북촌 골목의 감성적인 분위기 속 식사'
WHERE restaurant_id = (
    SELECT id FROM restaurants WHERE google_place_id = 'ChIJ4Q1Ca8WifDURRr2G9wHjgHc'
);
