package com.pickeat.pickeatbackend;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.review.repository.LikeNotification;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewLikeRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Limit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

// 좋아요 알림 쿼리를 실제 DB로 검증한다. 알림은 별도 테이블 없이 review_likes에서 만든다.
@SpringBootTest
@Transactional
class LikeNotificationQueryIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReviewLikeRepository reviewLikeRepository;

    @Test
    @DisplayName("내 후기에 다른 사람이 누른 좋아요만 최신순으로 알림이 되고, 취소하면 사라진다")
    void listsOthersLikesOnMyReviewsNewestFirst() {
        Long me = insertMember("나");
        Long alice = insertMember("앨리스");
        Long bob = insertMember("밥");
        Long restaurantId = insertRestaurant();
        Long myReview = insertReview(me, restaurantId);
        Long othersReview = insertReview(alice, restaurantId);

        insertLike(myReview, alice, "2 hours");
        insertLike(myReview, bob, "1 hour");
        insertLike(myReview, me, "10 minutes");
        insertLike(othersReview, bob, "5 minutes");

        List<LikeNotification> notifications = reviewLikeRepository.findLikeNotifications(me, Limit.of(50));

        assertThat(notifications).extracting(LikeNotification::actorNickname).containsExactly("밥", "앨리스");
        assertThat(notifications.get(0).reviewId()).isEqualTo(myReview);
        assertThat(notifications.get(0).restaurantName()).isEqualTo("알림 테스트 식당");

        reviewLikeRepository.deleteByReviewAndMember(myReview, bob);

        assertThat(reviewLikeRepository.findLikeNotifications(me, Limit.of(50)))
                .extracting(LikeNotification::actorNickname).containsExactly("앨리스");
    }

    private Long insertMember(String nickname) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO members (email, password, nickname, login_provider, created_at, updated_at)
                VALUES (?, 'password', ?, 'LOCAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """, Long.class, "notification-" + System.nanoTime() + "@pickeat.com", nickname);
    }

    private Long insertRestaurant() {
        return jdbcTemplate.queryForObject("""
                INSERT INTO restaurants (name, food_category, latitude, longitude)
                VALUES ('알림 테스트 식당', 'KOREAN', 37.58, 127.0)
                RETURNING id
                """, Long.class);
    }

    // 후기는 Pick 하나당 하나라서 후기마다 세션과 Pick을 새로 만든다.
    private Long insertReview(Long memberId, Long restaurantId) {
        Long sessionId = jdbcTemplate.queryForObject("""
                INSERT INTO recommendation_sessions (member_id, companion_type, latitude, longitude)
                VALUES (?, 'DATE', 37.58, 127.0)
                RETURNING id
                """, Long.class, memberId);
        Long pickId = jdbcTemplate.queryForObject("""
                INSERT INTO picks (member_id, restaurant_id, recommendation_session_id, companion_type, status)
                VALUES (?, ?, ?, 'DATE', 'REVIEWED')
                RETURNING id
                """, Long.class, memberId, restaurantId, sessionId);
        return jdbcTemplate.queryForObject("""
                INSERT INTO reviews (pick_id, member_id, restaurant_id, content, food_category,
                                     companion_type, visibility)
                VALUES (?, ?, ?, '맛있어요', 'KOREAN', 'SOLO', 'PUBLIC')
                RETURNING id
                """, Long.class, pickId, memberId, restaurantId);
    }

    private void insertLike(Long reviewId, Long memberId, String age) {
        jdbcTemplate.update("""
                INSERT INTO review_likes (review_id, member_id, created_at)
                VALUES (?, ?, CURRENT_TIMESTAMP - CAST(? AS INTERVAL))
                """, reviewId, memberId, age);
    }
}
