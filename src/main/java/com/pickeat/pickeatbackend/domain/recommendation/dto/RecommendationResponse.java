package com.pickeat.pickeatbackend.domain.recommendation.dto;

import com.pickeat.pickeatbackend.domain.recommendation.entity.RecommendationCandidate;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import java.math.BigDecimal;
import java.util.List;

public record RecommendationResponse(Long sessionId, List<Item> items) {

    public static RecommendationResponse of(Long sessionId, List<RecommendationCandidate> candidates) {
        return new RecommendationResponse(sessionId, candidates.stream().map(Item::from).toList());
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
        public static Item from(RecommendationCandidate candidate) {
            return new Item(
                    candidate.getRestaurant().getId(),
                    candidate.getRestaurant().getName(),
                    candidate.getRestaurant().getFoodCategory(),
                    candidate.getRestaurant().getExternalRating(),
                    candidate.getDistanceMeters(),
                    candidate.getResultRank(),
                    candidate.getTotalScore()
            );
        }
    }
}
