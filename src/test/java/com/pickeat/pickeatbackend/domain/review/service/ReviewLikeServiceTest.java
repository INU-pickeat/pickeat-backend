package com.pickeat.pickeatbackend.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pickeat.pickeatbackend.domain.pick.entity.Pick;
import com.pickeat.pickeatbackend.domain.review.ReviewFixtures;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewLikeResponse;
import com.pickeat.pickeatbackend.domain.review.entity.Review;
import com.pickeat.pickeatbackend.domain.review.entity.ReviewVisibility;
import com.pickeat.pickeatbackend.domain.review.exception.ReviewErrorCode;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewLikeRepository;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewLikeServiceTest {

    private static final Long AUTHOR_ID = 1L;
    private static final Long OTHER_ID = 2L;

    @Mock ReviewRepository reviewRepository;
    @Mock ReviewLikeRepository reviewLikeRepository;

    private ReviewLikeService reviewLikeService;
    private Pick pick;

    @BeforeEach
    void setUp() {
        reviewLikeService = new ReviewLikeService(reviewRepository, reviewLikeRepository);
        pick = ReviewFixtures.pick(
                30L, ReviewFixtures.member(AUTHOR_ID, "작성자"), ReviewFixtures.restaurant(10L, "테스트 식당"));
    }

    @Test
    @DisplayName("공개 후기에 좋아요를 누르면 좋아요 수를 돌려준다")
    void likesPublicReview() {
        Review review = ReviewFixtures.review(100L, pick, ReviewVisibility.PUBLIC, "공개", List.of());
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        when(reviewLikeRepository.countByReviewId(100L)).thenReturn(4L);

        ReviewLikeResponse response = reviewLikeService.like(100L, OTHER_ID);

        verify(reviewLikeRepository).insertIfAbsent(100L, OTHER_ID);
        assertThat(response.liked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("본인의 비공개 후기에는 좋아요를 누를 수 없다")
    void rejectsLikeOnOwnPrivateReview() {
        Review review = ReviewFixtures.review(100L, pick, ReviewVisibility.PRIVATE, "비공개", List.of());
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewLikeService.like(100L, AUTHOR_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReviewErrorCode.REVIEW_NOT_PUBLIC.getMessage());
        verify(reviewLikeRepository, never()).insertIfAbsent(any(), any());
    }

    @Test
    @DisplayName("남의 비공개 후기는 없는 후기로 응답한다")
    void hidesOthersPrivateReview() {
        Review review = ReviewFixtures.review(100L, pick, ReviewVisibility.PRIVATE, "비공개", List.of());
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewLikeService.like(100L, OTHER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReviewErrorCode.REVIEW_NOT_FOUND.getMessage());
        verify(reviewLikeRepository, never()).insertIfAbsent(any(), any());
    }

    @Test
    @DisplayName("좋아요를 취소하면 남은 좋아요 수를 돌려준다")
    void unlikesReview() {
        Review review = ReviewFixtures.review(100L, pick, ReviewVisibility.PUBLIC, "공개", List.of());
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(review));
        when(reviewLikeRepository.countByReviewId(100L)).thenReturn(0L);

        ReviewLikeResponse response = reviewLikeService.unlike(100L, OTHER_ID);

        verify(reviewLikeRepository).deleteByReviewAndMember(100L, OTHER_ID);
        assertThat(response.liked()).isFalse();
        assertThat(response.likeCount()).isZero();
    }
}
