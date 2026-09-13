package com.pickeat.pickeatbackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestDatabaseConfig.class)
@Transactional
class RestaurantSchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appliesV2AndCreatesSpatialIndex() {
        assertThat(jdbcTemplate.queryForObject(
            "SELECT success FROM flyway_schema_history WHERE version = '2'", Boolean.class))
            .isTrue();
        assertThat(jdbcTemplate.queryForObject("""
            SELECT indexdef FROM pg_indexes
            WHERE schemaname = 'public' AND indexname = 'restaurants_location_gist_idx'
            """, String.class)).contains("USING gist (location)");
    }

    @Test
    void preservesUnknownAndExplicitCompanionValues() {
        Long id = insertRestaurant();
        var values = jdbcTemplate.queryForMap("""
            SELECT suitable_for_date, suitable_for_friends, suitable_for_family,
                   suitable_for_solo, suitable_for_group_dinner
            FROM restaurants WHERE id = ?
            """, id);
        assertThat(values.values()).containsOnlyNulls();

        jdbcTemplate.update("""
            UPDATE restaurants SET suitable_for_date = true, suitable_for_solo = false WHERE id = ?
            """, id);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT suitable_for_date FROM restaurants WHERE id = ?", Boolean.class, id)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT suitable_for_solo FROM restaurants WHERE id = ?", Boolean.class, id)).isFalse();
    }

    @Test
    void generatesLocationAndKeepsItInSyncWithCoordinates() {
        Long id = insertRestaurant();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT ST_X(location::geometry) FROM restaurants WHERE id = ?", Double.class, id))
            .isEqualTo(127.0);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT ST_Y(location::geometry) FROM restaurants WHERE id = ?", Double.class, id))
            .isEqualTo(37.58);

        jdbcTemplate.update("UPDATE restaurants SET longitude = 127.01 WHERE id = ?", id);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT ST_X(location::geometry) FROM restaurants WHERE id = ?", Double.class, id))
            .isEqualTo(127.01);
    }

    @Test
    void radiusSearchIncludesNearbyAndExcludesBeyondFiveKilometers() {
        Long id = insertRestaurant();
        // 동일한 기준점에서 미터 단위로 이동한 위치로 반경의 안과 밖을 검증한다.
        assertThat(jdbcTemplate.queryForObject("""
            SELECT ST_DWithin(location, ST_Project(location, 4999, 0), 5000)
            FROM restaurants WHERE id = ?
            """, Boolean.class, id)).isTrue();
        assertThat(jdbcTemplate.queryForObject("""
            SELECT ST_DWithin(location, ST_Project(location, 5001, 0), 5000)
            FROM restaurants WHERE id = ?
            """, Boolean.class, id)).isFalse();
    }

    private Long insertRestaurant() {
        return jdbcTemplate.queryForObject("""
            INSERT INTO restaurants (name, latitude, longitude)
            VALUES ('테스트 전용 식당', 37.58, 127.0) RETURNING id
            """, Long.class);
    }
}
