package com.pickeat.pickeatbackend.domain.restaurant.repository;

import com.pickeat.pickeatbackend.TestDatabaseConfig;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestDatabaseConfig.class)
@Transactional
class RestaurantRepositoryTest {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void savesAndLoadsRestaurantFromDatabase() {
        Restaurant saved = restaurantRepository.saveAndFlush(Restaurant.builder()
            .name("테스트 식당").address("테스트 주소")
            .latitude(37.58).longitude(127.0).foodCategory("한식").build());
        entityManager.clear();
        Restaurant found = restaurantRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getName()).isEqualTo("테스트 식당");
        assertThat(found.getAddress()).isEqualTo("테스트 주소");
        assertThat(found.getLatitude()).isEqualTo(37.58);
        assertThat(found.getLongitude()).isEqualTo(127.0);
        assertThat(found.getFoodCategory()).isEqualTo("한식");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isEqualTo(found.getCreatedAt());
    }

    @Test
    void keepsUnknownFieldsNull() {
        Restaurant saved = restaurantRepository.saveAndFlush(Restaurant.builder()
            .name("최소 정보 식당").latitude(37.58).longitude(127.0).build());
        entityManager.clear();
        Restaurant found = restaurantRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getAddress()).isNull();
        assertThat(found.getFoodCategory()).isNull();
        assertThat(found.getSuitableForDate()).isNull();
        assertThat(found.getSuitableForFriends()).isNull();
        assertThat(found.getSuitableForFamily()).isNull();
        assertThat(found.getSuitableForSolo()).isNull();
        assertThat(found.getSuitableForGroupDinner()).isNull();
    }

    @Test
    void preservesConfirmedSuitabilityAndDatabaseGeneratedLocation() {
        Restaurant saved = restaurantRepository.saveAndFlush(Restaurant.builder()
            .name("동행 확인 식당").latitude(37.58).longitude(127.0)
            .suitableForDate(true).suitableForFriends(false).suitableForFamily(true)
            .suitableForSolo(false).suitableForGroupDinner(true).build());
        entityManager.clear();
        Restaurant found = restaurantRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getSuitableForDate()).isTrue();
        assertThat(found.getSuitableForFriends()).isFalse();
        assertThat(found.getSuitableForFamily()).isTrue();
        assertThat(found.getSuitableForSolo()).isFalse();
        assertThat(found.getSuitableForGroupDinner()).isTrue();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT ST_X(location::geometry) FROM restaurants WHERE id = ?",
            Double.class, saved.getId())).isEqualTo(127.0);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT ST_Y(location::geometry) FROM restaurants WHERE id = ?",
            Double.class, saved.getId())).isEqualTo(37.58);
    }

    @Test
    void returnsEmptyWhenRestaurantDoesNotExist() {
        assertThat(restaurantRepository.findById(-1L)).isEmpty();
    }

    @Test
    void rejectsCoordinatesOutsideDatabaseRange() {
        Restaurant restaurant = Restaurant.builder()
            .name("잘못된 좌표 식당").latitude(91.0).longitude(127.0).build();

        assertThatThrownBy(() -> restaurantRepository.saveAndFlush(restaurant))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
