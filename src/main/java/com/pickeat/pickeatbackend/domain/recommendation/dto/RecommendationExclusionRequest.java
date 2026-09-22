package com.pickeat.pickeatbackend.domain.recommendation.dto;

import com.pickeat.pickeatbackend.domain.recommendation.entity.ExclusionReason;
import jakarta.validation.constraints.NotNull;

public record RecommendationExclusionRequest(
        @NotNull Long restaurantId,
        @NotNull ExclusionReason reason
) {
}
