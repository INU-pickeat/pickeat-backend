-- 홈 상단 "기록하러 가볼까요?" 배너를 사용자가 X로 닫은 시각. 닫은 Pick은 다시 배너에 띄우지 않는다.
ALTER TABLE picks ADD COLUMN review_prompt_dismissed_at TIMESTAMPTZ;
