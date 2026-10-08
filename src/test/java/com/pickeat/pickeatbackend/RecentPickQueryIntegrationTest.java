package com.pickeat.pickeatbackend;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.pick.repository.PickRepository;
import com.pickeat.pickeatbackend.domain.pick.repository.RestaurantPickSummary;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

// 최근 Pick 집계 쿼리를 실제 DB로 검증한다. 서비스 테스트는 저장소를 목으로 대체해서 JPQL 자체는 확인하지 못한다.
@SpringBootTest
@Transactional
class RecentPickQueryIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PickRepository pickRepository;

    @Test
    @DisplayName("최근 Pick은 식당별로 묶고 취소·기간 밖·다른 회원의 Pick은 제외한다")
    void groupsRecentPicksByRestaurant() {
        Long memberId = insertMember();
        Long otherMemberId = insertMember();
        Long curated = insertRestaurant("큐레이션 식당", "JAPANESE", null, "/images/discovery/sinsa_01_main.jpg");
        Long google = insertRestaurant("구글 식당", "KOREAN", "place-" + System.nanoTime(), null);
        Long canceledOnly = insertRestaurant("취소한 식당", "KOREAN", null, null);
        Long oldOnly = insertRestaurant("오래된 식당", "KOREAN", null, null);

        insertPick(memberId, curated, "SELECTED", "3 days");
        insertPick(memberId, curated, "REVIEWED", "2 days");
        insertPick(memberId, google, "SELECTED", "1 hour");
        insertPick(memberId, canceledOnly, "CANCELED", "1 hour");
        insertPick(memberId, oldOnly, "SELECTED", "10 days");
        insertPick(otherMemberId, google, "SELECTED", "1 hour");

        List<RestaurantPickSummary> summaries =
                pickRepository.findRecentPickSummaries(memberId, Instant.now().minus(Duration.ofDays(7)));

        assertThat(summaries).extracting(RestaurantPickSummary::restaurantId).containsExactly(google, curated);
        RestaurantPickSummary googleSummary = summaries.get(0);
        assertThat(googleSummary.pickCount()).isEqualTo(1L);
        assertThat(googleSummary.foodCategory()).isEqualTo(FoodCategory.KOREAN);
        assertThat(googleSummary.googlePlaceId()).startsWith("place-");
        assertThat(googleSummary.representativeImageUrl()).isNull();
        RestaurantPickSummary curatedSummary = summaries.get(1);
        assertThat(curatedSummary.pickCount()).isEqualTo(2L);
        assertThat(curatedSummary.foodCategory()).isEqualTo(FoodCategory.JAPANESE);
        assertThat(curatedSummary.representativeImageUrl()).isEqualTo("/images/discovery/sinsa_01_main.jpg");
    }

    private Long insertMember() {
        return jdbcTemplate.queryForObject("""
                INSERT INTO members (email, password, nickname, login_provider, created_at, updated_at)
                VALUES (?, 'password', '테스터', 'LOCAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """, Long.class, "recent-pick-" + System.nanoTime() + "@pickeat.com");
    }

    private Long insertRestaurant(String name, String foodCategory, String googlePlaceId, String imageUrl) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO restaurants (name, food_category, latitude, longitude, google_place_id,
                                         representative_image_url)
                VALUES (?, ?, 37.58, 127.0, ?, ?)
                RETURNING id
                """, Long.class, name, foodCategory, googlePlaceId, imageUrl);
    }

    // 추천 세션 하나에는 Pick을 하나만 만들 수 있어서 Pick마다 세션을 새로 만든다.
    private void insertPick(Long memberId, Long restaurantId, String status, String age) {
        Long sessionId = jdbcTemplate.queryForObject("""
                INSERT INTO recommendation_sessions (member_id, companion_type, latitude, longitude)
                VALUES (?, 'DATE', 37.58, 127.0)
                RETURNING id
                """, Long.class, memberId);
        jdbcTemplate.update("""
                INSERT INTO picks (member_id, restaurant_id, recommendation_session_id, companion_type, status,
                                   selected_at)
                VALUES (?, ?, ?, 'DATE', ?, CURRENT_TIMESTAMP - CAST(? AS INTERVAL))
                """, memberId, restaurantId, sessionId, status, age);
    }
}
