package com.pickeat.pickeatbackend.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pickeat.pickeatbackend.domain.member.entity.Member;
import com.pickeat.pickeatbackend.domain.pick.entity.Pick;
import com.pickeat.pickeatbackend.domain.pick.entity.PickStatus;
import com.pickeat.pickeatbackend.domain.pick.dto.PickPeriod;
import com.pickeat.pickeatbackend.domain.pick.exception.PickErrorCode;
import com.pickeat.pickeatbackend.domain.pick.repository.PickRepository;
import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.domain.review.ReviewFixtures;
import com.pickeat.pickeatbackend.domain.review.dto.CreateReviewRequest;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewImageUploadRequest;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewImageUploadResponse;
import com.pickeat.pickeatbackend.domain.review.dto.MyReviewsResponse;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewResponse;
import com.pickeat.pickeatbackend.domain.review.dto.UpdateReviewRequest;
import com.pickeat.pickeatbackend.domain.review.entity.Review;
import com.pickeat.pickeatbackend.domain.review.entity.ReviewVisibility;
import com.pickeat.pickeatbackend.domain.review.exception.ReviewErrorCode;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewLikeRepository;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewRepository;
import com.pickeat.pickeatbackend.domain.review.storage.ReviewImageStorage;
import com.pickeat.pickeatbackend.domain.review.storage.ReviewImageStorage.PresignedUpload;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final String OWN_IMAGE = "https://img.pickeat.kr/reviews/1/a.jpg";

    @Mock ReviewRepository reviewRepository;
    @Mock ReviewLikeRepository reviewLikeRepository;
    @Mock PickRepository pickRepository;
    @Mock ReviewImageStorage reviewImageStorage;

    private ReviewService reviewService;
    private Member member;
    private Restaurant restaurant;
    private Pick pick;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, reviewLikeRepository, pickRepository, reviewImageStorage);
        member = ReviewFixtures.member(MEMBER_ID, "작성자");
        restaurant = ReviewFixtures.restaurant(10L, "테스트 식당");
        pick = ReviewFixtures.pick(30L, member, restaurant);
    }

    @Test
    @DisplayName("후기를 작성하면 Pick이 REVIEWED로 바뀌고 방문 시각이 기록된다")
    void createsReviewAndMarksPickReviewed() {
        when(pickRepository.findByIdAndMemberId(30L, MEMBER_ID)).thenReturn(Optional.of(pick));
        when(reviewRepository.existsByPickId(30L)).thenReturn(false);
        when(reviewImageStorage.isUploadedBy(MEMBER_ID, OWN_IMAGE)).thenReturn(true);
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(invocation -> {
            Review saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        ReviewResponse response = reviewService.create(createRequest(List.of(OWN_IMAGE)), MEMBER_ID);

        assertThat(response.reviewId()).isEqualTo(100L);
        assertThat(response.pickId()).isEqualTo(30L);
        assertThat(response.restaurantName()).isEqualTo("테스트 식당");
        assertThat(response.foodCategory()).isEqualTo(FoodCategory.JAPANESE);
        assertThat(response.companionType()).isEqualTo(CompanionType.FAMILY);
        assertThat(response.visibility()).isEqualTo(ReviewVisibility.PUBLIC);
        assertThat(response.imageUrls()).containsExactly(OWN_IMAGE);
        assertThat(response.likeCount()).isZero();
        assertThat(pick.getStatus()).isEqualTo(PickStatus.REVIEWED);
        assertThat(pick.getVisitedAt()).isNotNull();
    }

    @Test
    @DisplayName("남의 Pick이나 없는 Pick에는 후기를 쓸 수 없다")
    void rejectsReviewForUnknownPick() {
        when(pickRepository.findByIdAndMemberId(30L, MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.create(createRequest(List.of()), MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(PickErrorCode.PICK_NOT_FOUND.getMessage());
        verify(reviewRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Pick 하나에 후기는 하나만 쓸 수 있다")
    void rejectsSecondReviewForSamePick() {
        when(pickRepository.findByIdAndMemberId(30L, MEMBER_ID)).thenReturn(Optional.of(pick));
        when(reviewRepository.existsByPickId(30L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.create(createRequest(List.of()), MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReviewErrorCode.REVIEW_ALREADY_EXISTS.getMessage());
        assertThat(pick.getStatus()).isEqualTo(PickStatus.SELECTED);
    }

    @Test
    @DisplayName("취소한 Pick에는 후기를 쓸 수 없다")
    void rejectsReviewForCanceledPick() {
        pick.changeStatus(PickStatus.CANCELED);
        when(pickRepository.findByIdAndMemberId(30L, MEMBER_ID)).thenReturn(Optional.of(pick));
        when(reviewRepository.existsByPickId(30L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.create(createRequest(List.of()), MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(PickErrorCode.INVALID_STATUS_TRANSITION.getMessage());
        verify(reviewRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("업로드 URL로 올린 본인 이미지가 아니면 첨부를 거부하고 Pick 상태도 바꾸지 않는다")
    void rejectsForeignImageUrl() {
        when(pickRepository.findByIdAndMemberId(30L, MEMBER_ID)).thenReturn(Optional.of(pick));
        when(reviewRepository.existsByPickId(30L)).thenReturn(false);
        when(reviewImageStorage.isUploadedBy(MEMBER_ID, "https://evil.example.com/x.jpg")).thenReturn(false);

        assertThatThrownBy(() ->
                reviewService.create(createRequest(List.of("https://evil.example.com/x.jpg")), MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReviewErrorCode.INVALID_IMAGE_URL.getMessage());
        assertThat(pick.getStatus()).isEqualTo(PickStatus.SELECTED);
    }

    @Test
    @DisplayName("남의 비공개 후기는 없는 것처럼 처리하고, 공개 후기는 조회할 수 있다")
    void hidesPrivateReviewFromOthers() {
        Review privateReview = ReviewFixtures.review(100L, pick, ReviewVisibility.PRIVATE, "비공개", List.of());
        Review publicReview = ReviewFixtures.review(101L, pick, ReviewVisibility.PUBLIC, "공개", List.of());
        when(reviewRepository.findDetailById(100L)).thenReturn(Optional.of(privateReview));
        when(reviewRepository.findDetailById(101L)).thenReturn(Optional.of(publicReview));
        when(reviewLikeRepository.countByReviewId(101L)).thenReturn(3L);
        when(reviewLikeRepository.existsByReviewIdAndMemberId(101L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.get(100L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReviewErrorCode.REVIEW_NOT_FOUND.getMessage());

        ReviewResponse response = reviewService.get(101L, 2L);
        assertThat(response.likeCount()).isEqualTo(3L);
        assertThat(response.likedByMe()).isTrue();
    }

    @Test
    @DisplayName("나의 기록은 Pick 선택 시각 기준 기간의 후기 목록을 반환한다")
    void getsMyReviewsByPickPeriod() {
        Instant selectedAt = Instant.parse("2026-10-04T02:00:00Z");
        ReflectionTestUtils.setField(pick, "selectedAt", selectedAt);
        Review review = ReviewFixtures.review(100L, pick, ReviewVisibility.PRIVATE, "맛있어요", List.of(OWN_IMAGE));
        when(reviewRepository.findByMemberIdAndPickSelectedAtGreaterThanEqualOrderByPickSelectedAtDescIdDesc(
                org.mockito.ArgumentMatchers.eq(MEMBER_ID), any(Instant.class)))
                .thenReturn(List.of(review));

        MyReviewsResponse response = reviewService.getMine(MEMBER_ID, PickPeriod.WEEK);

        assertThat(response.reviews()).singleElement().satisfies(item -> {
            assertThat(item.reviewId()).isEqualTo(100L);
            assertThat(item.pickId()).isEqualTo(30L);
            assertThat(item.selectedAt()).isEqualTo(selectedAt);
            assertThat(item.visibility()).isEqualTo(ReviewVisibility.PRIVATE);
            assertThat(item.imageUrls()).containsExactly(OWN_IMAGE);
        });
    }

    @Test
    @DisplayName("본인 후기를 부분 수정한다")
    void updatesOwnReview() {
        Review review = ReviewFixtures.review(100L, pick, ReviewVisibility.PUBLIC, "맛있어요", List.of(OWN_IMAGE));
        when(reviewRepository.findByIdAndMemberId(100L, MEMBER_ID)).thenReturn(Optional.of(review));

        ReviewResponse response = reviewService.update(
                100L, new UpdateReviewRequest("정말 맛있어요", null, null, ReviewVisibility.PRIVATE, null), MEMBER_ID);

        assertThat(response.content()).isEqualTo("정말 맛있어요");
        assertThat(response.visibility()).isEqualTo(ReviewVisibility.PRIVATE);
        assertThat(response.imageUrls()).containsExactly(OWN_IMAGE);
    }

    @Test
    @DisplayName("남의 후기는 수정·삭제할 수 없다")
    void rejectsUpdateAndDeleteOfOthersReview() {
        when(reviewRepository.findByIdAndMemberId(100L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.update(
                100L, new UpdateReviewRequest(null, null, null, null, null), 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReviewErrorCode.REVIEW_NOT_FOUND.getMessage());
        assertThatThrownBy(() -> reviewService.delete(100L, 2L))
                .isInstanceOf(BusinessException.class);
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    @DisplayName("후기를 삭제하면 Pick이 SELECTED로 돌아가 지도·캘린더에서 빠진다")
    void deletesReviewAndRevertsPick() {
        pick.changeStatus(PickStatus.REVIEWED);
        Review review = ReviewFixtures.review(100L, pick, ReviewVisibility.PUBLIC, "맛있어요", List.of());
        when(reviewRepository.findByIdAndMemberId(100L, MEMBER_ID)).thenReturn(Optional.of(review));

        reviewService.delete(100L, MEMBER_ID);

        verify(reviewRepository).delete(review);
        assertThat(pick.getStatus()).isEqualTo(PickStatus.SELECTED);
        assertThat(pick.getVisitedAt()).isNull();
    }

    @Test
    @DisplayName("업로드 URL을 발급하면 올릴 주소와 저장될 이미지 주소를 돌려준다")
    void createsUploadUrl() {
        Instant expiresAt = Instant.parse("2026-10-04T03:10:00Z");
        when(reviewImageStorage.createUpload(MEMBER_ID, "image/jpeg"))
                .thenReturn(new PresignedUpload("https://s3/upload-a", OWN_IMAGE, expiresAt));

        ReviewImageUploadResponse response = reviewService.createImageUploads(
                new ReviewImageUploadRequest(List.of("image/jpeg")), MEMBER_ID);

        assertThat(response.uploads()).hasSize(1);
        assertThat(response.uploads().get(0).uploadUrl()).isEqualTo("https://s3/upload-a");
        assertThat(response.uploads().get(0).imageUrl()).isEqualTo(OWN_IMAGE);
        assertThat(response.uploads().get(0).contentType()).isEqualTo("image/jpeg");
        assertThat(response.uploads().get(0).expiresAt()).isEqualTo(expiresAt);
    }

    private CreateReviewRequest createRequest(List<String> imageUrls) {
        return new CreateReviewRequest(
                30L, "양고기가 부드러워요", FoodCategory.JAPANESE, CompanionType.FAMILY,
                ReviewVisibility.PUBLIC, imageUrls);
    }
}
