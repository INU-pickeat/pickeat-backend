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
import com.pickeat.pickeatbackend.domain.review.entity.ReviewVisibility;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewRepository;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewStats;
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
    @DisplayName("공개 후기 수·평균 별점(소수 첫째 자리)·최근 후기 한줄평을 돌려준다")
    void summarizesPublicReviews() {
        Review latest = ReviewFixtures.review(
                101L,
                ReviewFixtures.pick(30L, ReviewFixtures.member(1L, "작성자"), ReviewFixtures.restaurant(10L, "테스트 식당")),
                ReviewVisibility.PUBLIC, "양고기가 부드러워요\n또 올게요", List.of());
        when(restaurantRepository.existsById(10L)).thenReturn(true);
        when(reviewRepository.findPublicStats(10L)).thenReturn(new ReviewStats(3L, 4.3333333));
        when(reviewRepository.findLatestPublicByRestaurantIds(List.of(10L))).thenReturn(List.of(latest));

        ReviewSummaryResponse response = reviewSummaryService.getSummary(10L);

        assertThat(response.reviewCount()).isEqualTo(3L);
        assertThat(response.averageRating()).isEqualTo(4.3);
        assertThat(response.oneLineReview()).isEqualTo("양고기가 부드러워요");
    }

    @Test
    @DisplayName("후기가 없으면 0건·평균 null·'후기가 없습니다.'를 돌려준다")
    void returnsEmptySummary() {
        when(restaurantRepository.existsById(10L)).thenReturn(true);
        when(reviewRepository.findPublicStats(10L)).thenReturn(new ReviewStats(0L, null));
        when(reviewRepository.findLatestPublicByRestaurantIds(List.of(10L))).thenReturn(List.of());

        ReviewSummaryResponse response = reviewSummaryService.getSummary(10L);

        assertThat(response.reviewCount()).isZero();
        assertThat(response.averageRating()).isNull();
        assertThat(response.oneLineReview()).isEqualTo("후기가 없습니다.");
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
