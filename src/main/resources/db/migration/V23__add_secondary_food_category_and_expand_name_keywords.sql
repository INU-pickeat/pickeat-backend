-- 보조 음식 카테고리 컬럼을 추가하고, 확장한 이름 키워드로 Google 출처 식당의 카테고리를 다시 맞춘다.
-- 규칙은 FoodCategoryNameOverride와 같다(2026-10-08 시점 키워드): 가장 긴 키워드가 이기고, 길이가 같으면
-- 키워드 파일에서 위에 적힌 것이 이긴다. 그래서 짧은 키워드부터 적용하고 이기는 키워드를 마지막에 적용한다.
-- 운영자가 고른 탐색 스팟(CURATED)은 건드리지 않는다.

ALTER TABLE restaurants
    ADD COLUMN secondary_food_category VARCHAR(30);

ALTER TABLE restaurants
    ADD CONSTRAINT restaurants_secondary_food_category
        CHECK (secondary_food_category IS NULL OR secondary_food_category IN (
            'KOREAN', 'JAPANESE', 'CHINESE', 'WESTERN', 'CAFE_DESSERT', 'PUB_BAR', 'OTHER'
        ));

COMMENT ON COLUMN restaurants.secondary_food_category IS '두 카테고리에 걸치는 식당의 두 번째 카테고리(치킨: KOREAN+PUB_BAR 등). 없으면 NULL';

UPDATE restaurants SET food_category = 'OTHER', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%타코%';
UPDATE restaurants SET food_category = 'OTHER', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%커리%';
UPDATE restaurants SET food_category = 'PUB_BAR', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%술집%';
UPDATE restaurants SET food_category = 'PUB_BAR', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%주점%';
UPDATE restaurants SET food_category = 'PUB_BAR', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%맥주%';
UPDATE restaurants SET food_category = 'PUB_BAR', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%호프%';
UPDATE restaurants SET food_category = 'PUB_BAR', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%포차%';
UPDATE restaurants SET food_category = 'CAFE_DESSERT', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%빙수%';
UPDATE restaurants SET food_category = 'CAFE_DESSERT', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%도넛%';
UPDATE restaurants SET food_category = 'CAFE_DESSERT', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%커피%';
UPDATE restaurants SET food_category = 'CAFE_DESSERT', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%카페%';
UPDATE restaurants SET food_category = 'WESTERN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%버거%';
UPDATE restaurants SET food_category = 'WESTERN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%피자%';
UPDATE restaurants SET food_category = 'JAPANESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%텐동%';
UPDATE restaurants SET food_category = 'JAPANESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%소바%';
UPDATE restaurants SET food_category = 'JAPANESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%우동%';
UPDATE restaurants SET food_category = 'JAPANESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%라멘%';
UPDATE restaurants SET food_category = 'JAPANESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%초밥%';
UPDATE restaurants SET food_category = 'JAPANESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%스시%';
UPDATE restaurants SET food_category = 'CHINESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%딤섬%';
UPDATE restaurants SET food_category = 'CHINESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%반점%';
UPDATE restaurants SET food_category = 'CHINESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%중화%';
UPDATE restaurants SET food_category = 'CHINESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%짬뽕%';
UPDATE restaurants SET food_category = 'CHINESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%짜장%';
UPDATE restaurants SET food_category = 'CHINESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%훠궈%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = 'PUB_BAR' WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%치킨%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%만두%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%국수%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%분식%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%김밥%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%파전%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%막회%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%횟집%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%물회%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%백숙%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%한우%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%쌈밥%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%육회%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%백반%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%낙지%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%막창%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%곱창%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%갈비%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%족발%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%보쌈%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%냉면%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%곰탕%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%순대%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%국밥%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%장어%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%찜닭%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%게장%';
UPDATE restaurants SET food_category = 'OTHER', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%팟타이%';
UPDATE restaurants SET food_category = 'OTHER', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%쌀국수%';
UPDATE restaurants SET food_category = 'PUB_BAR', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%와인바%';
UPDATE restaurants SET food_category = 'CAFE_DESSERT', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%케이크%';
UPDATE restaurants SET food_category = 'CAFE_DESSERT', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%디저트%';
UPDATE restaurants SET food_category = 'WESTERN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%리조또%';
UPDATE restaurants SET food_category = 'WESTERN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%브런치%';
UPDATE restaurants SET food_category = 'WESTERN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%파스타%';
UPDATE restaurants SET food_category = 'JAPANESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%돈카츠%';
UPDATE restaurants SET food_category = 'JAPANESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%사케동%';
UPDATE restaurants SET food_category = 'JAPANESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%규카츠%';
UPDATE restaurants SET food_category = 'CHINESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%우육면%';
UPDATE restaurants SET food_category = 'CHINESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%탕수육%';
UPDATE restaurants SET food_category = 'CHINESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%마라탕%';
UPDATE restaurants SET food_category = 'CHINESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%양꼬치%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%돈가스%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%돈까스%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%떡볶이%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%빈대떡%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%코다리%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%낙곱새%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%청국장%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%순두부%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%육개장%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%불고기%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%보리밥%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%비빔밥%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%한정식%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%쭈꾸미%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%해물찜%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%아구찜%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%삼겹살%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%막국수%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%추어탕%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%설렁탕%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%감자탕%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%해장국%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%삼계탕%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%닭갈비%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%수제비%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%칼국수%';
UPDATE restaurants SET food_category = 'CAFE_DESSERT', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%베이커리%';
UPDATE restaurants SET food_category = 'WESTERN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%비스트로%';
UPDATE restaurants SET food_category = 'WESTERN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%스테이크%';
UPDATE restaurants SET food_category = 'JAPANESE', secondary_food_category = 'PUB_BAR' WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%이자카야%';
UPDATE restaurants SET food_category = 'JAPANESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%타코야끼%';
UPDATE restaurants SET food_category = 'JAPANESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%오마카세%';
UPDATE restaurants SET food_category = 'CHINESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%꿔바로우%';
UPDATE restaurants SET food_category = 'CHINESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%마라샹궈%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%조개구이%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%생선구이%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%닭볶음탕%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%된장찌개%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%김치찌개%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%부대찌개%';
UPDATE restaurants SET food_category = 'KOREAN', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%닭한마리%';
UPDATE restaurants SET food_category = 'CHINESE', secondary_food_category = NULL WHERE data_provider = 'GOOGLE' AND REPLACE(name, ' ', '') ILIKE '%샤오롱바오%';
