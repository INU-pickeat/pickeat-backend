package com.pickeat.pickeatbackend.domain.recommendation.dto;

import com.pickeat.pickeatbackend.domain.recommendation.entity.RecommendationCandidate;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

public record RecommendationResponse(Long sessionId, List<Item> items) {

    // rank는 세션에 저장된 고정 순위가 아니라 현재 노출 목록 안에서의 위치다. 제외 후
    // 대체 후보가 빈 자리를 채우면 원래 저장 순위와 무관하게 그 자리의 순위로 보인다.
    public static RecommendationResponse of(Long sessionId, List<RecommendationCandidate> candidates) {
        List<Item> items = IntStream.range(0, candidates.size())
                .mapToObj(i -> Item.from(candidates.get(i), i + 1))
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
            double score
    ) {
        public static Item from(RecommendationCandidate candidate, int rank) {
            return new Item(
                    candidate.getRestaurant().getId(),
                    candidate.getRestaurant().getName(),
                    candidate.getRestaurant().getFoodCategory(),
                    candidate.getRestaurant().getExternalRating(),
                    candidate.getDistanceMeters(),
                    rank,
                    candidate.getTotalScore()
            );
        }
    }
}
