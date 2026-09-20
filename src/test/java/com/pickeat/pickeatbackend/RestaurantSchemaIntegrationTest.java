package com.pickeat.pickeatbackend;

import static org.assertj.core.api.Assertions.assertThat;

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
    void restaurant_마이그레이션과_공간_인덱스가_적용된다() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '5'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '6'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT indexdef FROM pg_indexes
                WHERE schemaname = 'public' AND indexname = 'restaurants_location_gist_idx'
                """, String.class)).contains("USING gist (location)");
    }

    @Test
    void 위치를_자동_생성하고_5km_반경을_판정한다() {
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
}
