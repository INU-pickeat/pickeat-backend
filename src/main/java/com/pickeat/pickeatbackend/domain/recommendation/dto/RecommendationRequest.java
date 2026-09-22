package com.pickeat.pickeatbackend.domain.recommendation.dto;

import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Set;

public record RecommendationRequest(
        @NotEmpty Set<FoodCategory> foodCategories,
        @NotNull CompanionType companionType,
        @Valid PriceRange priceRange,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude
) {

    // 생략/null은 가격 무관으로 처리한다. RecommendationService.matchesPriceRange() 참고.
    public record PriceRange(
            @NotNull @PositiveOrZero BigDecimal min,
            @NotNull @PositiveOrZero BigDecimal max
    ) {
        @AssertTrue(message = "min은 max보다 클 수 없습니다.")
        public boolean isValidRange() {
            return min == null || max == null || min.compareTo(max) <= 0;
        }
    }
}
