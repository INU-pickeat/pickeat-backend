package com.pickeat.pickeatbackend.domain.review.service;

import com.pickeat.pickeatbackend.domain.pick.dto.PickPeriod;
import com.pickeat.pickeatbackend.domain.pick.entity.Pick;
import com.pickeat.pickeatbackend.domain.pick.entity.PickStatus;
import com.pickeat.pickeatbackend.domain.pick.exception.PickErrorCode;
import com.pickeat.pickeatbackend.domain.pick.repository.PickRepository;
import com.pickeat.pickeatbackend.domain.review.dto.CreateReviewRequest;
import com.pickeat.pickeatbackend.domain.review.dto.MyReviewsResponse;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewImageUploadRequest;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewImageUploadResponse;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewResponse;
import com.pickeat.pickeatbackend.domain.review.dto.UpdateReviewRequest;
import com.pickeat.pickeatbackend.domain.review.entity.Review;
import com.pickeat.pickeatbackend.domain.review.exception.ReviewErrorCode;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewLikeRepository;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewRepository;
import com.pickeat.pickeatbackend.domain.review.storage.ReviewImageStorage;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final PickRepository pickRepository;
    private final ReviewImageStorage reviewImageStorage;

    // 후기 작성이 Pick을 REVIEWED로 바꾸는 유일한 경로다. 이때 방문 시각이 기록되어 지도·캘린더에 노출된다.
    @Transactional
    public ReviewResponse create(CreateReviewRequest request, Long memberId) {
        Pick pick = pickRepository.findByIdAndMemberId(request.pickId(), memberId)
                .orElseThrow(() -> new BusinessException(PickErrorCode.PICK_NOT_FOUND));
        if (reviewRepository.existsByPickId(pick.getId())) {
            throw new BusinessException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }
        validateOwnImages(request.imageUrls(), memberId);
        // SELECTED → REVIEWED. CANCELED Pick이면 PICK_004로 거부된다.
        pick.changeStatus(PickStatus.REVIEWED);

        Review review = Review.builder()
                .pick(pick)
                .content(request.content())
                .foodCategory(request.foodCategory())
                .companionType(request.companionType())
                .visibility(request.visibility())
                .imageUrls(request.imageUrls())
                .build();
        try {
            return ReviewResponse.of(reviewRepository.saveAndFlush(review), 0, false);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }
    }

    // 본인 후기는 공개 여부와 관계없이, 남의 후기는 공개일 때만 조회할 수 있다.
    @Transactional(readOnly = true)
    public ReviewResponse get(Long reviewId, Long memberId) {
        Review review = reviewRepository.findDetailById(reviewId)
                .filter(found -> found.isPublic() || found.isWrittenBy(memberId))
                .orElseThrow(() -> new BusinessException(ReviewErrorCode.REVIEW_NOT_FOUND));
        return toResponse(review, memberId);
    }

    @Transactional(readOnly = true)
    public MyReviewsResponse getMine(Long memberId, PickPeriod period) {
        Instant since = Instant.now().minus(period.window());
        return MyReviewsResponse.from(
                reviewRepository.findByMemberIdAndPickSelectedAtGreaterThanEqualOrderByPickSelectedAtDescIdDesc(
                        memberId, since));
    }

    @Transactional
    public ReviewResponse update(Long reviewId, UpdateReviewRequest request, Long memberId) {
        Review review = reviewRepository.findByIdAndMemberId(reviewId, memberId)
                .orElseThrow(() -> new BusinessException(ReviewErrorCode.REVIEW_NOT_FOUND));
        review.update(request.content(), request.foodCategory(), request.companionType(),
                request.visibility());
        return toResponse(review, memberId);
    }

    // 후기를 지우면 Pick은 SELECTED로 돌아가 지도·캘린더에서 빠지고, 다시 후기를 쓸 수 있다.
    @Transactional
    public void delete(Long reviewId, Long memberId) {
        Review review = reviewRepository.findByIdAndMemberId(reviewId, memberId)
                .orElseThrow(() -> new BusinessException(ReviewErrorCode.REVIEW_NOT_FOUND));
        Pick pick = review.getPick();
        reviewRepository.delete(review);
        pick.revertReview();
    }

    public ReviewImageUploadResponse createImageUploads(ReviewImageUploadRequest request, Long memberId) {
        List<ReviewImageUploadResponse.Upload> uploads = request.contentTypes().stream()
                .map(contentType -> ReviewImageUploadResponse.Upload.of(
                        reviewImageStorage.createUpload(memberId, contentType), contentType))
                .toList();
        return new ReviewImageUploadResponse(uploads);
    }

    private void validateOwnImages(List<String> imageUrls, Long memberId) {
        boolean allOwn = imageUrls.stream().allMatch(url -> reviewImageStorage.isUploadedBy(memberId, url));
        if (!allOwn) {
            throw new BusinessException(ReviewErrorCode.INVALID_IMAGE_URL);
        }
    }

    private ReviewResponse toResponse(Review review, Long memberId) {
        return ReviewResponse.of(
                review,
                reviewLikeRepository.countByReviewId(review.getId()),
                reviewLikeRepository.existsByReviewIdAndMemberId(review.getId(), memberId));
    }
}
