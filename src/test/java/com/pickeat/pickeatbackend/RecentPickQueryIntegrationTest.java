package com.pickeat.pickeatbackend;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.pick.entity.Pick;
import com.pickeat.pickeatbackend.domain.pick.entity.PickStatus;
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

    @Test
    @DisplayName("최근 Pick 삭제는 내 그 식당의 SELECTED Pick만 취소하고 REVIEWED와 다른 식당·회원은 그대로 둔다")
    void cancelsOnlyMySelectedPicksForRestaurant() {
        Long memberId = insertMember();
        Long otherMemberId = insertMember();
        Long target = insertRestaurant("지울 식당", "KOREAN", null, null);
        Long other = insertRestaurant("남길 식당", "KOREAN", null, null);
        insertPick(memberId, target, "SELECTED", "1 hour");
        insertPick(memberId, target, "SELECTED", "40 days");
        insertPick(memberId, target, "REVIEWED", "2 days");
        insertPick(memberId, other, "SELECTED", "1 hour");
        insertPick(otherMemberId, target, "SELECTED", "1 hour");

        assertThat(pickRepository.cancelSelected(memberId, target, Instant.now())).isEqualTo(2);

        assertThat(jdbcTemplate.queryForList("""
                SELECT status FROM picks WHERE member_id = ? AND restaurant_id = ? ORDER BY status
                """, String.class, memberId, target)).containsExactly("CANCELED", "CANCELED", "REVIEWED");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM picks WHERE status = 'SELECTED' AND (restaurant_id = ? OR member_id = ?)
                """, Long.class, other, otherMemberId)).isEqualTo(2L);
    }

    @Test
    @DisplayName("후기 작성 유도 배너는 7일 안의 SELECTED·미닫힘 Pick 중 가장 최근 1건이고, 최신이 기록됐으면 그 이전 Pick이다")
    void findsLatestPromptablePick() {
        Long memberId = insertMember();
        Long otherMemberId = insertMember();
        Long restaurant = insertRestaurant("배너 식당", "KOREAN", null, null);
        insertPick(memberId, restaurant, "REVIEWED", "10 minutes");
        Long dismissed = insertPick(memberId, restaurant, "SELECTED", "20 minutes");
        insertPick(memberId, restaurant, "CANCELED", "30 minutes");
        Long expected = insertPick(memberId, restaurant, "SELECTED", "2 days");
        insertPick(memberId, restaurant, "SELECTED", "3 days");
        insertPick(memberId, restaurant, "SELECTED", "8 days");
        insertPick(otherMemberId, restaurant, "SELECTED", "1 minute");
        jdbcTemplate.update("UPDATE picks SET review_prompt_dismissed_at = CURRENT_TIMESTAMP WHERE id = ?", dismissed);

        Instant since = Instant.now().minus(Duration.ofDays(7));
        assertThat(pickRepository
                .findFirstByMemberIdAndStatusAndReviewPromptDismissedAtIsNullAndSelectedAtGreaterThanEqualOrderBySelectedAtDescIdDesc(
                        memberId, PickStatus.SELECTED, since))
                .map(Pick::getId)
                .contains(expected);

        Long onlyOld = insertMember();
        insertPick(onlyOld, restaurant, "SELECTED", "8 days");
        assertThat(pickRepository
                .findFirstByMemberIdAndStatusAndReviewPromptDismissedAtIsNullAndSelectedAtGreaterThanEqualOrderBySelectedAtDescIdDesc(
                        onlyOld, PickStatus.SELECTED, since))
                .isEmpty();
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
    private Long insertPick(Long memberId, Long restaurantId, String status, String age) {
        Long sessionId = jdbcTemplate.queryForObject("""
                INSERT INTO recommendation_sessions (member_id, companion_type, latitude, longitude)
                VALUES (?, 'DATE', 37.58, 127.0)
                RETURNING id
                """, Long.class, memberId);
        return jdbcTemplate.queryForObject("""
                INSERT INTO picks (member_id, restaurant_id, recommendation_session_id, companion_type, status,
                                   selected_at)
                VALUES (?, ?, ?, 'DATE', ?, CURRENT_TIMESTAMP - CAST(? AS INTERVAL))
                RETURNING id
                """, Long.class, memberId, restaurantId, sessionId, status, age);
    }
}
