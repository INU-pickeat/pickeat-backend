package com.pickeat.pickeatbackend.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pickeat.pickeatbackend.domain.pick.entity.Pick;
import com.pickeat.pickeatbackend.domain.review.ReviewFixtures;
import com.pickeat.pickeatbackend.domain.review.dto.FeedResponse;
import com.pickeat.pickeatbackend.domain.review.entity.Review;
import com.pickeat.pickeatbackend.domain.review.entity.ReviewVisibility;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewLikeCount;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewLikeRepository;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ReviewFeedServiceTest {

    private static final Long VIEWER_ID = 2L;

    @Mock ReviewRepository reviewRepository;
    @Mock ReviewLikeRepository reviewLikeRepository;

    private ReviewFeedService reviewFeedService;
    private Pick pick;

    @BeforeEach
    void setUp() {
        reviewFeedService = new ReviewFeedService(reviewRepository, reviewLikeRepository);
        pick = ReviewFixtures.pick(
                30L, ReviewFixtures.member(1L, "작성자"), ReviewFixtures.restaurant(10L, "테스트 식당"));
    }

    @Test
    @DisplayName("첫 페이지는 한 건 더 읽어 다음 커서를 만들고, 좋아요 수와 내 좋아요 여부를 채운다")
    void returnsFirstPageWithNextCursor() {
        Review newest = ReviewFixtures.review(103L, pick, ReviewVisibility.PUBLIC, "셋째", List.of("https://img/c.jpg"));
        Review middle = ReviewFixtures.review(102L, pick, ReviewVisibility.PUBLIC, "둘째", List.of());
        Review extra = ReviewFixtures.review(101L, pick, ReviewVisibility.PUBLIC, "첫째", List.of());
        when(reviewRepository.findPublicFeed(any(Pageable.class))).thenReturn(List.of(newest, middle, extra));
        when(reviewLikeRepository.countByReviewIds(List.of(103L, 102L)))
                .thenReturn(List.of(new ReviewLikeCount(103L, 5L)));
        when(reviewLikeRepository.findLikedReviewIds(VIEWER_ID, List.of(103L, 102L))).thenReturn(List.of(103L));

        FeedResponse response = reviewFeedService.getFeed(VIEWER_ID, null, 2);

        ArgumentCaptor<Pageable> limit = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewRepository).findPublicFeed(limit.capture());
        assertThat(limit.getValue().getPageSize()).isEqualTo(3);
        assertThat(response.items()).extracting(FeedResponse.Item::reviewId).containsExactly(103L, 102L);
        assertThat(response.nextCursor()).isEqualTo(102L);
        assertThat(response.items().get(0).likeCount()).isEqualTo(5L);
        assertThat(response.items().get(0).likedByMe()).isTrue();
        assertThat(response.items().get(0).authorNickname()).isEqualTo("작성자");
        assertThat(response.items().get(0).imageUrls()).containsExactly("https://img/c.jpg");
        assertThat(response.items().get(1).likeCount()).isZero();
        assertThat(response.items().get(1).likedByMe()).isFalse();
    }

    @Test
    @DisplayName("커서가 있으면 그보다 오래된 후기를 읽고, 더 없으면 다음 커서는 null이다")
    void returnsLastPageWithoutNextCursor() {
        Review last = ReviewFixtures.review(101L, pick, ReviewVisibility.PUBLIC, "첫째", List.of());
        when(reviewRepository.findPublicFeedBefore(eq(102L), any(Pageable.class))).thenReturn(List.of(last));
        when(reviewLikeRepository.countByReviewIds(List.of(101L))).thenReturn(List.of());
        when(reviewLikeRepository.findLikedReviewIds(VIEWER_ID, List.of(101L))).thenReturn(List.of());

        FeedResponse response = reviewFeedService.getFeed(VIEWER_ID, 102L, 2);

        assertThat(response.items()).hasSize(1);
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("공개 후기가 없으면 빈 목록을 돌려주고 좋아요는 조회하지 않는다")
    void returnsEmptyFeed() {
        when(reviewRepository.findPublicFeed(any(Pageable.class))).thenReturn(List.of());

        FeedResponse response = reviewFeedService.getFeed(VIEWER_ID, null, 20);

        assertThat(response.items()).isEmpty();
        assertThat(response.nextCursor()).isNull();
        verifyNoInteractions(reviewLikeRepository);
    }

    @Test
    @DisplayName("페이지 크기는 1~50, 커서는 1 이상이어야 한다")
    void rejectsInvalidPaging() {
        assertThatThrownBy(() -> reviewFeedService.getFeed(VIEWER_ID, null, 0)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> reviewFeedService.getFeed(VIEWER_ID, null, 51)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> reviewFeedService.getFeed(VIEWER_ID, 0L, 20)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(reviewRepository);
    }
}
