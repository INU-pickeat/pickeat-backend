package com.pickeat.pickeatbackend.domain.review.service;

import com.pickeat.pickeatbackend.domain.review.dto.ReviewLikeResponse;
import com.pickeat.pickeatbackend.domain.review.entity.Review;
import com.pickeat.pickeatbackend.domain.review.exception.ReviewErrorCode;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewLikeRepository;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewLikeService {

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;

    // 좋아요는 공개 피드에 올라간(PUBLIC) 후기에만 누를 수 있다. 여러 번 눌러도 한 번으로 센다.
    @Transactional
    public ReviewLikeResponse like(Long reviewId, Long memberId) {
        Review review = findVisible(reviewId, memberId);
        if (!review.isPublic()) {
            throw new BusinessException(ReviewErrorCode.REVIEW_NOT_PUBLIC);
        }
        reviewLikeRepository.insertIfAbsent(reviewId, memberId);
        return new ReviewLikeResponse(reviewId, true, reviewLikeRepository.countByReviewId(reviewId));
    }

    // 취소는 후기가 나중에 비공개로 바뀌었어도 허용한다. 누른 적이 없어도 같은 결과를 돌려준다.
    @Transactional
    public ReviewLikeResponse unlike(Long reviewId, Long memberId) {
        findVisible(reviewId, memberId);
        reviewLikeRepository.deleteByReviewAndMember(reviewId, memberId);
        return new ReviewLikeResponse(reviewId, false, reviewLikeRepository.countByReviewId(reviewId));
    }

    // 남의 비공개 후기는 존재 자체를 알리지 않는다.
    private Review findVisible(Long reviewId, Long memberId) {
        return reviewRepository.findById(reviewId)
                .filter(review -> review.isPublic() || review.isWrittenBy(memberId))
                .orElseThrow(() -> new BusinessException(ReviewErrorCode.REVIEW_NOT_FOUND));
    }
}
