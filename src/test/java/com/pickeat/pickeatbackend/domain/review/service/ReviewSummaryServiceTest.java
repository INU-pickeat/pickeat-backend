package com.pickeat.pickeatbackend.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pickeat.pickeatbackend.domain.restaurant.exception.RestaurantErrorCode;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import com.pickeat.pickeatbackend.domain.review.ReviewFixtures;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewSummaryResponse;
import com.pickeat.pickeatbackend.domain.review.entity.Review;
import com.pickeat.pickeatbackend.domain.review.entity.ReviewKeyword;
import com.pickeat.pickeatbackend.domain.review.entity.ReviewVisibility;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewKeywordCount;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewSummaryServiceTest {

    @Mock ReviewRepository reviewRepository;
    @Mock RestaurantRepository restaurantRepository;

    private ReviewSummaryService reviewSummaryService;

    @BeforeEach
    void setUp() {
        reviewSummaryService = new ReviewSummaryService(reviewRepository, restaurantRepository);
    }

    @Test
    @DisplayName("공개 후기 수와 최근 후기 한줄평을 돌려준다")
    void summarizesPublicReviews() {
        Review latest = ReviewFixtures.review(
                101L,
                ReviewFixtures.pick(30L, ReviewFixtures.member(1L, "작성자"), ReviewFixtures.restaurant(10L, "테스트 식당")),
                ReviewVisibility.PUBLIC, "양고기가 부드러워요\n또 올게요", List.of());
        when(restaurantRepository.existsById(10L)).thenReturn(true);
        when(reviewRepository.countByRestaurantIdAndVisibility(10L, ReviewVisibility.PUBLIC)).thenReturn(3L);
        when(reviewRepository.findLatestPublicByRestaurantIds(List.of(10L))).thenReturn(List.of(latest));

        ReviewSummaryResponse response = reviewSummaryService.getSummary(10L);

        assertThat(response.reviewCount()).isEqualTo(3L);
        assertThat(response.oneLineReview()).isEqualTo("양고기가 부드러워요");
    }

    @Test
    @DisplayName("후기가 없으면 0건과 '후기가 없습니다.'를 돌려준다")
    void returnsEmptySummary() {
        when(restaurantRepository.existsById(10L)).thenReturn(true);
        when(reviewRepository.countByRestaurantIdAndVisibility(10L, ReviewVisibility.PUBLIC)).thenReturn(0L);
        when(reviewRepository.findLatestPublicByRestaurantIds(List.of(10L))).thenReturn(List.of());

        ReviewSummaryResponse response = reviewSummaryService.getSummary(10L);

        assertThat(response.reviewCount()).isZero();
        assertThat(response.oneLineReview()).isEqualTo("후기가 없습니다.");
        assertThat(response.topKeywords()).isEmpty();
    }

    @Test
    @DisplayName("상위 키워드는 건수 내림차순, 같으면 enum 선언 순서로 5개까지 돌려준다")
    void ranksTopKeywordsByCountThenDeclarationOrder() {
        when(restaurantRepository.existsById(10L)).thenReturn(true);
        when(reviewRepository.countPublicKeywordsByRestaurantId(10L)).thenReturn(List.of(
                new ReviewKeywordCount(ReviewKeyword.DOG_FRIENDLY, 1L),
                new ReviewKeywordCount(ReviewKeyword.CLEAN, 3L),
                new ReviewKeywordCount(ReviewKeyword.GOOD_VALUE, 2L),
                new ReviewKeywordCount(ReviewKeyword.TASTY, 3L),
                new ReviewKeywordCount(ReviewKeyword.KIND_SERVICE, 1L),
                new ReviewKeywordCount(ReviewKeyword.FRESH_INGREDIENTS, 1L)));

        ReviewSummaryResponse response = reviewSummaryService.getSummary(10L);

        assertThat(response.topKeywords()).containsExactly(
                new ReviewSummaryResponse.KeywordCount("TASTY", "음식이 맛있어요", 3),
                new ReviewSummaryResponse.KeywordCount("CLEAN", "매장이 청결해요", 3),
                new ReviewSummaryResponse.KeywordCount("GOOD_VALUE", "가성비가 좋아요", 2),
                new ReviewSummaryResponse.KeywordCount("FRESH_INGREDIENTS", "재료가 신선해요", 1),
                new ReviewSummaryResponse.KeywordCount("KIND_SERVICE", "친절해요", 1));
    }

    @Test
    @DisplayName("없는 식당은 RESTAURANT_001로 거부한다")
    void rejectsUnknownRestaurant() {
        when(restaurantRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> reviewSummaryService.getSummary(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(RestaurantErrorCode.RESTAURANT_NOT_FOUND.getMessage());
        verifyNoInteractions(reviewRepository);
    }

    @Test
    @DisplayName("여러 식당의 한줄평은 후기가 있는 식당만 맵에 담는다")
    void mapsOneLinersOnlyForReviewedRestaurants() {
        Review latest = ReviewFixtures.review(
                101L,
                ReviewFixtures.pick(30L, ReviewFixtures.member(1L, "작성자"), ReviewFixtures.restaurant(10L, "테스트 식당")),
                ReviewVisibility.PUBLIC, "분위기가 좋아요", List.of());
        when(reviewRepository.findLatestPublicByRestaurantIds(List.of(10L, 11L))).thenReturn(List.of(latest));

        Map<Long, String> oneLiners = reviewSummaryService.getOneLineReviews(List.of(10L, 11L));

        assertThat(oneLiners).containsOnly(Map.entry(10L, "분위기가 좋아요"));
        assertThat(reviewSummaryService.getOneLineReviews(List.of())).isEmpty();
    }
}
