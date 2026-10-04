package com.pickeat.pickeatbackend.domain.review.service;

import com.pickeat.pickeatbackend.domain.restaurant.exception.RestaurantErrorCode;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewOneLiner;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewSummaryResponse;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewRepository;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewStats;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 식당별 후기 집계. 공개 후기만 대상으로 하며, 대표 한줄평은 가장 최근 공개 후기의 첫 줄이다.
@Service
@RequiredArgsConstructor
public class ReviewSummaryService {

    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getSummary(Long restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND);
        }
        ReviewStats stats = reviewRepository.findPublicStats(restaurantId);
        long reviewCount = stats == null || stats.reviewCount() == null ? 0 : stats.reviewCount();
        Double averageRating = stats == null || stats.averageRating() == null
                ? null
                : Math.round(stats.averageRating() * 10) / 10.0;
        String oneLineReview = getOneLineReviews(List.of(restaurantId))
                .getOrDefault(restaurantId, ReviewOneLiner.NO_REVIEW);
        return new ReviewSummaryResponse(restaurantId, reviewCount, averageRating, oneLineReview);
    }

    // 후기가 있는 식당만 맵에 들어간다. 없는 식당은 호출한 쪽에서 ReviewOneLiner.NO_REVIEW로 채운다.
    @Transactional(readOnly = true)
    public Map<Long, String> getOneLineReviews(Collection<Long> restaurantIds) {
        if (restaurantIds.isEmpty()) {
            return Map.of();
        }
        return reviewRepository.findLatestPublicByRestaurantIds(restaurantIds).stream()
                .collect(Collectors.toMap(
                        review -> review.getRestaurant().getId(),
                        review -> ReviewOneLiner.from(review.getContent())));
    }
}
