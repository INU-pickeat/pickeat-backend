package com.pickeat.pickeatbackend.domain.restaurant.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RestaurantRepositoryIntegrationTest {

    private static final double CENTER_LATITUDE = 37.58;
    private static final double CENTER_LONGITUDE = 127.0;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 반경_5km_밖의_식당은_제외되고_안쪽은_포함된다() {
        double[] inside = pointAtDistance(4999);
        double[] outside = pointAtDistance(5001);
        insertRestaurant("경계_안쪽", inside[0], inside[1]);
        insertRestaurant("경계_바깥쪽", outside[0], outside[1]);

        List<RestaurantCandidate> candidates =
                restaurantRepository.findWithinRadius(CENTER_LATITUDE, CENTER_LONGITUDE, 5000);

        assertThat(candidates).extracting(c -> c.restaurant().getName()).containsExactly("경계_안쪽");
    }

    @Test
    void 가까운_식당부터_거리순으로_정렬된다() {
        double[] far = pointAtDistance(4000);
        double[] near = pointAtDistance(1000);
        insertRestaurant("먼_식당", far[0], far[1]);
        insertRestaurant("가까운_식당", near[0], near[1]);

        List<RestaurantCandidate> candidates =
                restaurantRepository.findWithinRadius(CENTER_LATITUDE, CENTER_LONGITUDE, 5000);

        assertThat(candidates).extracting(c -> c.restaurant().getName())
                .containsExactly("가까운_식당", "먼_식당");
        assertThat(candidates.get(0).distanceMeters()).isLessThan(candidates.get(1).distanceMeters());
    }

    @Test
    void 반경_안에_후보가_없어도_자동으로_넓히지_않는다() {
        double[] withinFiveKm = pointAtDistance(3000);
        insertRestaurant("5km_이내_식당", withinFiveKm[0], withinFiveKm[1]);

        List<RestaurantCandidate> candidates =
                restaurantRepository.findWithinRadius(CENTER_LATITUDE, CENTER_LONGITUDE, 100);

        assertThat(candidates).isEmpty();
    }

    private double[] pointAtDistance(double distanceMeters) {
        return jdbcTemplate.queryForObject("""
                SELECT ST_Y(pt::geometry), ST_X(pt::geometry) FROM (
                    SELECT ST_Project(
                        ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?, 0
                    ) AS pt
                ) projected
                """, (rs, rowNum) -> new double[]{rs.getDouble(1), rs.getDouble(2)},
                CENTER_LONGITUDE, CENTER_LATITUDE, distanceMeters);
    }

    private void insertRestaurant(String name, double latitude, double longitude) {
        jdbcTemplate.update("""
                INSERT INTO restaurants (name, food_category, latitude, longitude)
                VALUES (?, 'KOREAN', ?, ?)
                """, name, latitude, longitude);
    }
}
