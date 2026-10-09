package com.pickeat.pickeatbackend;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RestaurantSchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("restaurant 마이그레이션과 공간 인덱스가 적용된다")
    void appliesRestaurantMigrationAndSpatialIndex() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '5'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '6'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '9'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '13'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '14'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '15'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '25'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT indexdef FROM pg_indexes
                WHERE schemaname = 'public' AND indexname = 'restaurants_location_gist_idx'
                """, String.class)).contains("USING gist (location)");
    }

    @Test
    @DisplayName("영업시간 배열 컬럼을 저장하고 다시 읽는다")
    void storesAndReadsOpeningWeekMinutes() {
        Restaurant restaurant = restaurantRepository.saveAndFlush(Restaurant.builder()
                .name("영업시간 테스트 식당")
                .latitude(37.5)
                .longitude(127.0)
                .build());
        restaurant.updateOpeningHours(new int[] {4980, 5580, 5700, 6000});
        restaurantRepository.saveAndFlush(restaurant);
        entityManager.clear();

        assertThat(restaurantRepository.findById(restaurant.getId()).orElseThrow().getOpeningWeekMinutes())
                .containsExactly(4980, 5580, 5700, 6000);
    }

    @Test
    @DisplayName("최종 5개 탐색 스팟을 고정 순서로 저장한다")
    void storesFinalDiscoverySpotsInDisplayOrder() {
        assertThat(jdbcTemplate.queryForList("""
                SELECT region_code FROM discovery_spots ORDER BY display_order
                """, String.class))
                .containsExactly("SINSA", "HYEHWA", "SEOCHON", "HANNAM", "JONGNO");
    }

    @Test
    @DisplayName("최종 25개 큐레이션 식당을 지역별 5곳씩 저장한다")
    void storesFiveCuratedRestaurantsPerDiscoverySpot() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM restaurants WHERE data_provider = 'CURATED'
                """, Integer.class)).isGreaterThanOrEqualTo(25);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM discovery_spot_restaurants
                """, Integer.class)).isEqualTo(25);
        assertThat(jdbcTemplate.queryForList("""
                SELECT COUNT(dsr.id)
                FROM discovery_spots ds
                LEFT JOIN discovery_spot_restaurants dsr ON dsr.discovery_spot_id = ds.id
                GROUP BY ds.id, ds.display_order
                ORDER BY ds.display_order
                """, Integer.class)).containsExactly(5, 5, 5, 5, 5);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM restaurants r
                JOIN discovery_spot_restaurants dsr ON dsr.restaurant_id = r.id
                WHERE r.google_place_id IS NULL OR r.location IS NULL
                """, Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM restaurants r
                JOIN discovery_spot_restaurants dsr ON dsr.restaurant_id = r.id
                WHERE r.representative_image_url LIKE '/images/discovery/%_main.jpg'
                """, Integer.class)).isEqualTo(25);
    }

    @Test
    @DisplayName("탐색 스팟 식당·순서·사진이 확정 목록(discovery-spots.csv)과 같다")
    void discoverySpotsMatchCuratedCsv() throws Exception {
        List<String> expected;
        try (var in = new ClassPathResource("curated/discovery-spots.csv").getInputStream()) {
            expected = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .skip(1)
                    .filter(line -> !line.isBlank())
                    .map(line -> line.split(",", -1))
                    .map(cols -> cols[0] + "|" + cols[1] + "|" + cols[2].replace("\"", "")
                            + "|/images/discovery/" + cols[cols.length - 1].replace("\"", ""))
                    .toList();
        }

        List<String> actual = jdbcTemplate.queryForList("""
                SELECT ds.region_code || '|' || dsr.display_order || '|' || r.name || '|' || r.representative_image_url
                FROM discovery_spot_restaurants dsr
                JOIN discovery_spots ds ON ds.id = dsr.discovery_spot_id
                JOIN restaurants r ON r.id = dsr.restaurant_id
                ORDER BY ds.display_order, dsr.display_order
                """, String.class);

        assertThat(actual).containsExactlyElementsOf(expected);
    }

    @Test
    @DisplayName("탐색 스팟에서 빠진 식당은 탐색 사진을 쓰지 않는다")
    void removedDiscoveryRestaurantsDoNotKeepDiscoveryImages() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM restaurants r
                WHERE r.representative_image_url LIKE '/images/discovery/%'
                  AND NOT EXISTS (SELECT 1 FROM discovery_spot_restaurants dsr WHERE dsr.restaurant_id = r.id)
                """, Integer.class)).isZero();
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
