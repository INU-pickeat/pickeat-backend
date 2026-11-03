package com.pickeat.pickeatbackend.domain.review.service;

import com.pickeat.pickeatbackend.domain.restaurant.exception.RestaurantErrorCode;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewOneLiner;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewSummaryResponse;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewSummaryResponse.KeywordCount;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewKeywordCount;
import com.pickeat.pickeatbackend.domain.review.entity.ReviewVisibility;
import com.pickeat.pickeatbackend.domain.review.repository.ReviewRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 식당별 후기 집계. 공개 후기만 대상으로 하며, 대표 한줄평은 본문이 있는 가장 최근 공개 후기의 첫 줄이다.
@Service
@RequiredArgsConstructor
public class ReviewSummaryService {

    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;

    static final int TOP_KEYWORD_LIMIT = 5;

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getSummary(Long restaurantId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND);
        }
        long reviewCount = reviewRepository.countByRestaurantIdAndVisibility(
                restaurantId, ReviewVisibility.PUBLIC);
        String oneLineReview = getOneLineReviews(List.of(restaurantId))
                .getOrDefault(restaurantId, ReviewOneLiner.NO_REVIEW);
        return new ReviewSummaryResponse(restaurantId, reviewCount, oneLineReview, getTopKeywords(restaurantId));
    }

    // 건수 내림차순, 같으면 enum 선언 순서로 상위 5개.
    private List<KeywordCount> getTopKeywords(Long restaurantId) {
        return reviewRepository.countPublicKeywordsByRestaurantId(restaurantId).stream()
                .sorted(Comparator.comparing(ReviewKeywordCount::count).reversed()
                        .thenComparing(ReviewKeywordCount::keyword))
                .limit(TOP_KEYWORD_LIMIT)
                .map(count -> KeywordCount.of(count.keyword(), count.count()))
                .toList();
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
