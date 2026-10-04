package com.pickeat.pickeatbackend.domain.review.service;

import com.pickeat.pickeatbackend.domain.review.dto.FeedResponse;
import com.pickeat.pickeatbackend.domain.review.entity.Review;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewLikeCount;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewLikeRepository;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import com.pickeat.pickeatbackend.global.exception.GlobalErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewFeedService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 50;

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;

    // 공개 후기만 최신순으로 내려준다. cursor는 이전 응답의 nextCursor이며, 없으면 첫 페이지다.
    @Transactional(readOnly = true)
    public FeedResponse getFeed(Long memberId, Long cursor, int size) {
        if (size < 1 || size > MAX_PAGE_SIZE || (cursor != null && cursor < 1)) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT);
        }
        // 다음 페이지가 있는지 알기 위해 한 건 더 읽는다.
        Pageable limit = PageRequest.of(0, size + 1);
        List<Review> fetched = cursor == null
                ? reviewRepository.findPublicFeed(limit)
                : reviewRepository.findPublicFeedBefore(cursor, limit);
        boolean hasNext = fetched.size() > size;
        List<Review> page = hasNext ? fetched.subList(0, size) : fetched;
        if (page.isEmpty()) {
            return new FeedResponse(List.of(), null);
        }

        List<Long> reviewIds = page.stream().map(Review::getId).toList();
        Map<Long, Long> likeCounts = reviewLikeRepository.countByReviewIds(reviewIds).stream()
                .collect(Collectors.toMap(ReviewLikeCount::reviewId, ReviewLikeCount::likeCount));
        Set<Long> likedIds = new HashSet<>(reviewLikeRepository.findLikedReviewIds(memberId, reviewIds));

        List<FeedResponse.Item> items = page.stream()
                .map(review -> FeedResponse.Item.of(
                        review, likeCounts.getOrDefault(review.getId(), 0L), likedIds.contains(review.getId())))
                .toList();
        Long nextCursor = hasNext ? page.get(page.size() - 1).getId() : null;
        return new FeedResponse(items, nextCursor);
    }
}
