package com.pickeat.pickeatbackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ReviewSchemaIntegrationTest {

    private static final String INSERT_KEYWORD = "INSERT INTO review_keywords (review_id, keyword) VALUES (?, ?)";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Review 마이그레이션과 공개 피드 인덱스가 적용된다")
    void appliesReviewMigrationAndFeedIndex() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '19'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '27'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'reviews' AND column_name = 'rating'
                """, Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT indexdef FROM pg_indexes
                WHERE schemaname = 'public' AND indexname = 'reviews_public_feed_idx'
                """, String.class)).contains("id DESC", "PUBLIC");
    }

    @Test
    @DisplayName("Pick 하나에는 후기를 하나만 저장할 수 있다")
    void rejectsSecondReviewForPick() {
        TestIds ids = insertDependencies();
        insertReview(ids, "PUBLIC");

        assertThatThrownBy(() -> insertReview(ids, "PUBLIC"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("정의되지 않은 공개 범위는 저장할 수 없다")
    void rejectsUnknownVisibility() {
        TestIds ids = insertDependencies();

        assertThatThrownBy(() -> insertReview(ids, "FRIENDS"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 사용자는 한 후기에 좋아요를 한 번만 남길 수 있고, 중복 요청은 무시된다")
    void keepsSingleLikePerMember() {
        TestIds ids = insertDependencies();
        Long reviewId = insertReview(ids, "PUBLIC");
        String upsert = """
                INSERT INTO review_likes (review_id, member_id) VALUES (?, ?)
                ON CONFLICT (review_id, member_id) DO NOTHING
                """;

        assertThat(jdbcTemplate.update(upsert, reviewId, ids.memberId())).isEqualTo(1);
        assertThat(jdbcTemplate.update(upsert, reviewId, ids.memberId())).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM review_likes WHERE review_id = ?", Long.class, reviewId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("후기를 지우면 이미지와 좋아요도 함께 지워진다")
    void cascadesImagesAndLikesOnReviewDelete() {
        TestIds ids = insertDependencies();
        Long reviewId = insertReview(ids, "PUBLIC");
        jdbcTemplate.update(
                "INSERT INTO review_images (review_id, image_url, display_order) VALUES (?, 'https://img/a.jpg', 0)",
                reviewId);
        jdbcTemplate.update("INSERT INTO review_likes (review_id, member_id) VALUES (?, ?)", reviewId, ids.memberId());

        jdbcTemplate.update("DELETE FROM reviews WHERE id = ?", reviewId);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM review_images WHERE review_id = ?", Long.class, reviewId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM review_likes WHERE review_id = ?", Long.class, reviewId)).isZero();
    }

    @Test
    @DisplayName("V34: 본문은 NULL을 허용하지만 공백 본문은 여전히 막는다")
    void allowsNullContentButRejectsBlank() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '34'", Boolean.class)).isTrue();
        Long reviewId = insertReview(insertDependencies(), "PUBLIC", null);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT content FROM reviews WHERE id = ?", String.class, reviewId)).isNull();
        assertThatThrownBy(() -> insertReview(insertDependencies(), "PUBLIC", "   "))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("V34: 정의되지 않은 1인당 지출 구간은 저장할 수 없다")
    void rejectsUnknownSpendPerPerson() {
        Long reviewId = insertReview(insertDependencies(), "PUBLIC");
        String update = "UPDATE reviews SET spend_per_person = ? WHERE id = ?";

        assertThat(jdbcTemplate.update(update, "FROM_20000_TO_30000", reviewId)).isEqualTo(1);
        assertThatThrownBy(() -> jdbcTemplate.update(update, "OVER_50000", reviewId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("V34: 키워드는 후기마다 한 번씩만 저장된다")
    void rejectsDuplicateKeywordForReview() {
        Long reviewId = insertReview(insertDependencies(), "PUBLIC");
        jdbcTemplate.update(INSERT_KEYWORD, reviewId, "TASTY");

        assertThatThrownBy(() -> jdbcTemplate.update(INSERT_KEYWORD, reviewId, "TASTY"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("V34: 정의되지 않은 키워드는 저장할 수 없다")
    void rejectsUnknownKeyword() {
        Long reviewId = insertReview(insertDependencies(), "PUBLIC");

        assertThatThrownBy(() -> jdbcTemplate.update(INSERT_KEYWORD, reviewId, "SPICY"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("V34: 후기를 지우면 키워드도 함께 지워진다")
    void cascadesKeywordsOnReviewDelete() {
        Long reviewId = insertReview(insertDependencies(), "PUBLIC");
        jdbcTemplate.update(INSERT_KEYWORD, reviewId, "TASTY");

        jdbcTemplate.update("DELETE FROM reviews WHERE id = ?", reviewId);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM review_keywords WHERE review_id = ?", Long.class, reviewId)).isZero();
    }

    private TestIds insertDependencies() {
        Long memberId = jdbcTemplate.queryForObject("""
                INSERT INTO members (email, password, nickname, login_provider, created_at, updated_at)
                VALUES (?, 'password', '테스터', 'LOCAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """, Long.class, "review-" + System.nanoTime() + "@pickeat.com");
        Long restaurantId = jdbcTemplate.queryForObject("""
                INSERT INTO restaurants (name, food_category, latitude, longitude)
                VALUES ('Review 테스트 식당', 'KOREAN', 37.58, 127.0)
                RETURNING id
                """, Long.class);
        Long sessionId = jdbcTemplate.queryForObject("""
                INSERT INTO recommendation_sessions (member_id, companion_type, latitude, longitude)
                VALUES (?, 'DATE', 37.58, 127.0)
                RETURNING id
                """, Long.class, memberId);
        Long pickId = jdbcTemplate.queryForObject("""
                INSERT INTO picks (member_id, restaurant_id, recommendation_session_id, companion_type)
                VALUES (?, ?, ?, 'DATE')
                RETURNING id
                """, Long.class, memberId, restaurantId, sessionId);
        return new TestIds(memberId, restaurantId, pickId);
    }

    private Long insertReview(TestIds ids, String visibility) {
        return insertReview(ids, visibility, "맛있어요");
    }

    private Long insertReview(TestIds ids, String visibility, String content) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO reviews (pick_id, member_id, restaurant_id, content, food_category,
                                     companion_type, visibility)
                VALUES (?, ?, ?, ?, 'KOREAN', 'SOLO', ?)
                RETURNING id
                """, Long.class, ids.pickId(), ids.memberId(), ids.restaurantId(), content, visibility);
    }

    private record TestIds(Long memberId, Long restaurantId, Long pickId) {
    }
}
