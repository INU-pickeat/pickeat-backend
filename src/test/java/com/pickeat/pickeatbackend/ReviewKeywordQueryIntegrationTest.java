package com.pickeat.pickeatbackend;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.review.dto.ReviewSummaryResponse;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewSummaryResponse.KeywordCount;
import com.pickeat.pickeatbackend.domain.review.entity.ReviewKeyword;
import com.pickeat.pickeatbackend.domain.review.entity.SpendPerPerson;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewRepository;
import com.pickeat.pickeatbackend.domain.review.service.ReviewSummaryService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

// 후기 키워드 집계와 본문 없는 후기를 건너뛰는 한줄평 쿼리를 실제 DB로 검증한다.
@SpringBootTest
@Transactional
class ReviewKeywordQueryIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewSummaryService reviewSummaryService;

    @Test
    @DisplayName("식당 후기 요약의 상위 키워드는 공개 후기만 세고, 한줄평은 본문 없는 최신 후기를 건너뛴다")
    void summarizesPublicKeywordsAndSkipsReviewsWithoutContent() {
        Long memberId = insertMember();
        Long restaurantId = insertRestaurant();
        Long older = insertReview(memberId, restaurantId, "PUBLIC", "국물이 진해요\n또 올게요");
        Long latestWithoutContent = insertReview(memberId, restaurantId, "PUBLIC", null);
        Long privateReview = insertReview(memberId, restaurantId, "PRIVATE", "비공개 후기");
        insertKeywords(older, "TASTY", "CLEAN");
        insertKeywords(latestWithoutContent, "TASTY", "GOOD_VALUE");
        insertKeywords(privateReview, "CLEAN", "GOOD_VALUE", "DOG_FRIENDLY");

        ReviewSummaryResponse summary = reviewSummaryService.getSummary(restaurantId);

        assertThat(summary.reviewCount()).isEqualTo(2);
        assertThat(summary.oneLineReview()).isEqualTo("국물이 진해요");
        assertThat(summary.topKeywords()).containsExactly(
                KeywordCount.of(ReviewKeyword.TASTY, 2),
                KeywordCount.of(ReviewKeyword.GOOD_VALUE, 1),
                KeywordCount.of(ReviewKeyword.CLEAN, 1));
    }

    @Test
    @DisplayName("본문 있는 공개 후기가 없으면 한줄평 맵에서 빠진다")
    void omitsRestaurantWhenOnlyReviewsWithoutContent() {
        Long memberId = insertMember();
        Long restaurantId = insertRestaurant();
        insertReview(memberId, restaurantId, "PUBLIC", null);

        assertThat(reviewSummaryService.getOneLineReviews(List.of(restaurantId))).isEmpty();
    }

    @Test
    @DisplayName("엔티티는 저장된 키워드와 1인당 지출을 읽어온다")
    void loadsKeywordsAndSpendPerPerson() {
        Long reviewId = insertReview(insertMember(), insertRestaurant(), "PUBLIC", null);
        insertKeywords(reviewId, "DOG_FRIENDLY", "TASTY");
        jdbcTemplate.update("UPDATE reviews SET spend_per_person = 'OVER_30000' WHERE id = ?", reviewId);

        assertThat(reviewRepository.findById(reviewId)).hasValueSatisfying(review -> {
            assertThat(review.getContent()).isNull();
            assertThat(review.getSortedKeywords()).containsExactly(ReviewKeyword.TASTY, ReviewKeyword.DOG_FRIENDLY);
            assertThat(review.getSpendPerPerson()).isEqualTo(SpendPerPerson.OVER_30000);
        });
    }

    private Long insertMember() {
        return jdbcTemplate.queryForObject("""
                INSERT INTO members (email, password, nickname, login_provider, created_at, updated_at)
                VALUES (?, 'password', '키워드', 'LOCAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """, Long.class, "keyword-" + System.nanoTime() + "@pickeat.com");
    }

    private Long insertRestaurant() {
        return jdbcTemplate.queryForObject("""
                INSERT INTO restaurants (name, food_category, latitude, longitude)
                VALUES ('키워드 테스트 식당', 'KOREAN', 37.58, 127.0)
                RETURNING id
                """, Long.class);
    }

    // 후기는 Pick 하나당 하나라서 후기마다 세션과 Pick을 새로 만든다.
    private Long insertReview(Long memberId, Long restaurantId, String visibility, String content) {
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
                VALUES (?, ?, ?, ?, 'KOREAN', 'SOLO', ?)
                RETURNING id
                """, Long.class, pickId, memberId, restaurantId, content, visibility);
    }

    private void insertKeywords(Long reviewId, String... keywords) {
        for (String keyword : keywords) {
            jdbcTemplate.update("INSERT INTO review_keywords (review_id, keyword) VALUES (?, ?)", reviewId, keyword);
        }
    }
}
