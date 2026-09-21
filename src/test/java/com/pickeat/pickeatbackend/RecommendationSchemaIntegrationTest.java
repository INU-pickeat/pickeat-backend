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
class RecommendationSchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("recommendation 마이그레이션이 적용된다")
    void appliesRecommendationMigration() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '7'", Boolean.class)).isTrue();
    }

    @Test
    @DisplayName("세션과 카테고리와 후보를 저장하고 조회한다")
    void savesAndReadsSessionWithCategoriesAndCandidate() {
        Long memberId = insertMember();
        Long restaurantId = insertRestaurant();
        Long sessionId = insertSession(memberId);

        jdbcTemplate.update("""
                INSERT INTO recommendation_session_food_categories (session_id, food_category)
                VALUES (?, 'KOREAN')
                """, sessionId);

        jdbcTemplate.update("""
                INSERT INTO recommendation_candidates
                    (session_id, restaurant_id, result_rank, distance_meters,
                     rating_contribution, distance_contribution, companion_bonus, total_score)
                VALUES (?, ?, 1, 500, 0.6, 0.36, 0.1, 1.06)
                """, sessionId, restaurantId);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT total_score FROM recommendation_candidates WHERE session_id = ?",
                Double.class, sessionId)).isEqualTo(1.06);
    }

    @Test
    @DisplayName("순위가 1~5 범위를 벗어나면 거부된다")
    void rejectsRankOutsideOneToFiveRange() {
        Long memberId = insertMember();
        Long restaurantId = insertRestaurant();
        Long sessionId = insertSession(memberId);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO recommendation_candidates
                    (session_id, restaurant_id, result_rank, distance_meters,
                     rating_contribution, distance_contribution, companion_bonus, total_score)
                VALUES (?, ?, 6, 500, 0.6, 0.36, 0.1, 1.06)
                """, sessionId, restaurantId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 세션에 같은 식당이 두 번 추천되면 거부된다")
    void rejectsDuplicateRestaurantInSameSession() {
        Long memberId = insertMember();
        Long restaurantId = insertRestaurant();
        Long sessionId = insertSession(memberId);

        jdbcTemplate.update("""
                INSERT INTO recommendation_candidates
                    (session_id, restaurant_id, result_rank, distance_meters,
                     rating_contribution, distance_contribution, companion_bonus, total_score)
                VALUES (?, ?, 1, 500, 0.6, 0.36, 0.1, 1.06)
                """, sessionId, restaurantId);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO recommendation_candidates
                    (session_id, restaurant_id, result_rank, distance_meters,
                     rating_contribution, distance_contribution, companion_bonus, total_score)
                VALUES (?, ?, 2, 800, 0.5, 0.3, 0, 0.8)
                """, sessionId, restaurantId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Long insertMember() {
        return jdbcTemplate.queryForObject("""
                INSERT INTO members (email, password, nickname, login_provider, created_at, updated_at)
                VALUES (?, 'password', '테스터', 'LOCAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """, Long.class, "tester-" + System.nanoTime() + "@pickeat.com");
    }

    private Long insertRestaurant() {
        return jdbcTemplate.queryForObject("""
                INSERT INTO restaurants (name, food_category, latitude, longitude)
                VALUES ('테스트 식당', 'KOREAN', 37.58, 127.0)
                RETURNING id
                """, Long.class);
    }

    private Long insertSession(Long memberId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO recommendation_sessions (member_id, companion_type, latitude, longitude)
                VALUES (?, 'DATE', 37.58, 127.0)
                RETURNING id
                """, Long.class, memberId);
    }
}
