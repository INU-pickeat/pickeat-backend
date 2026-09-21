package com.pickeat.pickeatbackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void recommendation_마이그레이션이_적용된다() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '7'", Boolean.class)).isTrue();
    }

    @Test
    void 세션과_카테고리와_후보를_저장하고_조회한다() {
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
    void 순위가_1_5_범위를_벗어나면_거부된다() {
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
    void 같은_세션에_같은_식당이_두_번_추천되면_거부된다() {
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
