package com.pickeat.pickeatbackend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RestaurantSchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("restaurant 마이그레이션과 공간 인덱스가 적용된다")
    void appliesRestaurantMigrationAndSpatialIndex() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '5'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '6'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '9'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT indexdef FROM pg_indexes
                WHERE schemaname = 'public' AND indexname = 'restaurants_location_gist_idx'
                """, String.class)).contains("USING gist (location)");
    }

    @Test
    @DisplayName("위치를 자동 생성하고 5km 반경을 판정한다")
    void generatesLocationAndEvaluatesFiveKilometerRadius() {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO restaurants (name, food_category, latitude, longitude)
                VALUES ('테스트 식당', 'KOREAN', 37.58, 127.0)
                RETURNING id
                """, Long.class);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT ST_X(location::geometry) FROM restaurants WHERE id = ?",
                Double.class, id)).isEqualTo(127.0);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT ST_DWithin(location, ST_Project(location, 4999, 0), 5000)
                FROM restaurants WHERE id = ?
                """, Boolean.class, id)).isTrue();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT ST_DWithin(location, ST_Project(location, 5001, 0), 5000)
                FROM restaurants WHERE id = ?
                """, Boolean.class, id)).isFalse();
    }

    @Test
    @DisplayName("확장된 음식 카테고리와 동행 유형을 저장한다")
    void storesExpandedCategoriesAndCompanionTypes() {
        Long memberId = jdbcTemplate.queryForObject("""
                INSERT INTO members (email, password, nickname, created_at, updated_at)
                VALUES ('schema-v9@example.com', 'encoded', 'schema-v9', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """, Long.class);
        Long sessionId = jdbcTemplate.queryForObject("""
                INSERT INTO recommendation_sessions (member_id, companion_type, latitude, longitude)
                VALUES (?, 'DOG', 37.5, 127.0)
                RETURNING id
                """, Long.class, memberId);

        jdbcTemplate.update("""
                INSERT INTO recommendation_session_food_categories (session_id, food_category)
                VALUES (?, 'PUB_BAR'), (?, 'OTHER')
                """, sessionId, sessionId);

        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM recommendation_session_food_categories
                WHERE session_id = ?
                """, Integer.class, sessionId)).isEqualTo(2);
    }
}
