CREATE TABLE reviews (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pick_id         BIGINT NOT NULL REFERENCES picks (id),
    member_id       BIGINT NOT NULL REFERENCES members (id),
    restaurant_id   BIGINT NOT NULL REFERENCES restaurants (id),
    rating          INTEGER NOT NULL,
    content         VARCHAR(1000) NOT NULL,
    food_category   VARCHAR(30) NOT NULL,
    companion_type  VARCHAR(20) NOT NULL,
    visibility      VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT reviews_pick_unique UNIQUE (pick_id),
    CONSTRAINT reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT reviews_content_not_blank CHECK (LENGTH(TRIM(content)) > 0),
    CONSTRAINT reviews_food_category
        CHECK (food_category IN ('KOREAN', 'JAPANESE', 'CHINESE', 'WESTERN', 'CAFE_DESSERT', 'PUB_BAR', 'OTHER')),
    CONSTRAINT reviews_companion_type
        CHECK (companion_type IN ('DATE', 'FAMILY', 'CHILDREN', 'SOLO', 'GROUP', 'DOG')),
    CONSTRAINT reviews_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE'))
);

-- 공개 피드는 최신순(id DESC) 커서 조회, 식당별 요약·한줄평은 공개 후기만 집계한다.
CREATE INDEX reviews_public_feed_idx ON reviews (id DESC) WHERE visibility = 'PUBLIC';
CREATE INDEX reviews_restaurant_public_idx ON reviews (restaurant_id, id DESC) WHERE visibility = 'PUBLIC';
CREATE INDEX reviews_member_id_idx ON reviews (member_id);

CREATE TABLE review_images (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    review_id      BIGINT NOT NULL REFERENCES reviews (id) ON DELETE CASCADE,
    image_url      VARCHAR(500) NOT NULL,
    display_order  INTEGER NOT NULL,

    CONSTRAINT review_images_display_order CHECK (display_order >= 0)
);

CREATE INDEX review_images_review_order_idx ON review_images (review_id, display_order);

CREATE TABLE review_likes (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    review_id   BIGINT NOT NULL REFERENCES reviews (id) ON DELETE CASCADE,
    member_id   BIGINT NOT NULL REFERENCES members (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT review_likes_review_member_unique UNIQUE (review_id, member_id)
);

CREATE INDEX review_likes_member_id_idx ON review_likes (member_id);

COMMENT ON TABLE reviews IS 'Pick 하나당 하나만 쓸 수 있는 방문 후기. 작성 시 Pick이 REVIEWED로 전환된다';
COMMENT ON COLUMN reviews.food_category IS '후기 작성 화면에서 사용자가 직접 고른 음식 카테고리';
COMMENT ON COLUMN reviews.companion_type IS '후기 작성 화면에서 사용자가 직접 고른 동행 유형';
COMMENT ON COLUMN reviews.visibility IS 'PUBLIC만 공개 피드·좋아요·식당별 요약에 노출된다';
COMMENT ON COLUMN review_images.display_order IS '0부터 시작. 0번 이미지가 Pick 캘린더의 날짜 대표 이미지 후보다';
