-- Google 정기 영업시간. [여는 분, 닫는 분] 쌍을 펼친 배열(분 = 요일×1440 + 시×60 + 분, 0=일요일 0시).
-- NULL이면 영업시간을 모른다.
ALTER TABLE restaurants ADD COLUMN opening_week_minutes INTEGER[];
