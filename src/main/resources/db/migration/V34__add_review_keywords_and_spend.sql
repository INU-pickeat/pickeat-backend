-- 후기 본문을 선택 입력으로 바꾸고, "이런 점이 좋았어요" 키워드와 1인당 지출을 추가한다.
-- 기존 후기는 키워드·1인당 지출 없이 그대로 둔다.

-- 기존 reviews_content_not_blank CHECK는 NULL을 통과시키므로 그대로 두어 빈 본문 대신 NULL만 허용한다.
ALTER TABLE reviews ALTER COLUMN content DROP NOT NULL;

ALTER TABLE reviews ADD COLUMN spend_per_person VARCHAR(20);
ALTER TABLE reviews ADD CONSTRAINT reviews_spend_per_person
    CHECK (spend_per_person IN ('UNDER_10000', 'FROM_10000_TO_20000', 'FROM_20000_TO_30000', 'OVER_30000'));

CREATE TABLE review_keywords (
    review_id  BIGINT NOT NULL REFERENCES reviews (id) ON DELETE CASCADE,
    keyword    VARCHAR(30) NOT NULL,

    CONSTRAINT review_keywords_pk PRIMARY KEY (review_id, keyword),
    CONSTRAINT review_keywords_keyword CHECK (keyword IN (
        'TASTY', 'FRESH_INGREDIENTS', 'GENEROUS_PORTION', 'SIGNATURE_MENU', 'GOOD_VALUE',
        'NICE_ATMOSPHERE', 'CLEAN', 'COMFORTABLE_SEATING', 'GOOD_FOR_TALKING', 'KIND_SERVICE',
        'QUICK_SERVING', 'GOOD_FOR_DATE', 'GOOD_FOR_SOLO', 'GOOD_FOR_GROUP', 'GOOD_FOR_CHILDREN',
        'GOOD_FOR_FAMILY', 'DOG_FRIENDLY'))
);

COMMENT ON TABLE review_keywords IS '후기에서 고른 "이런 점이 좋았어요" 키워드. 후기 하나에 1~5개(애플리케이션에서 검증)';
COMMENT ON COLUMN review_keywords.keyword IS 'ReviewKeyword enum 값. 식당 후기 요약의 상위 키워드 집계에 쓴다';
COMMENT ON COLUMN reviews.content IS '후기 본문(선택). 없으면 NULL이며 빈 문자열·공백은 저장하지 않는다';
COMMENT ON COLUMN reviews.spend_per_person IS '작성자가 고른 1인당 지출 구간(선택). SpendPerPerson enum 값';
