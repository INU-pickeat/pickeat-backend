package com.pickeat.pickeatbackend.domain.recommendation.dto;

import com.pickeat.pickeatbackend.domain.recommendation.entity.RecommendationCandidate;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewOneLiner;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public record RecommendationResponse(Long sessionId, List<Item> items) {

    // rank는 세션에 저장된 고정 순위가 아니라 현재 노출 목록 안에서의 위치다. 제외 후
    // 대체 후보가 빈 자리를 채우면 원래 저장 순위와 무관하게 그 자리의 순위로 보인다.
    // oneLineReviews: restaurantId → 사용자 후기에서 뽑은 한줄평. 없는 식당은 ReviewOneLiner.NO_REVIEW로 채운다.
    public static RecommendationResponse of(
            Long sessionId, List<RecommendationCandidate> candidates, Map<Long, String> oneLineReviews) {
        List<Item> items = IntStream.range(0, candidates.size())
                .mapToObj(i -> Item.from(candidates.get(i), i + 1, oneLineReviews))
                .toList();
        return new RecommendationResponse(sessionId, items);
    }

    public record Item(
            Long restaurantId,
            String name,
            FoodCategory foodCategory,
            BigDecimal externalRating,
            double distanceMeters,
            int rank,
            double score,
            String representativeImageUrl,
            String oneLineReview
    ) {
        public static Item from(RecommendationCandidate candidate, int rank, Map<Long, String> oneLineReviews) {
            String oneLineReview = oneLineReviews.get(candidate.getRestaurant().getId());
            return new Item(
                    candidate.getRestaurant().getId(),
                    candidate.getRestaurant().getName(),
                    candidate.getRestaurant().getFoodCategory(),
                    candidate.getRestaurant().getExternalRating(),
                    candidate.getDistanceMeters(),
                    rank,
                    candidate.getTotalScore(),
                    candidate.getRestaurant().getRepresentativeImageUrl(),
                    oneLineReview == null || oneLineReview.isBlank() ? ReviewOneLiner.NO_REVIEW : oneLineReview
            );
        }
    }
}
