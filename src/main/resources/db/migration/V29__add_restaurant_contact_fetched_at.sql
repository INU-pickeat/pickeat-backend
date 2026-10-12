-- Google에서 전화번호·영업시간 텍스트를 받아 온 시각. NULL인 GOOGLE 식당이 백필 대상이다.
-- 값을 받지 못한 식당도 시각을 기록해 같은 식당을 다시 조회하지 않는다.
ALTER TABLE restaurants ADD COLUMN contact_fetched_at TIMESTAMPTZ;
