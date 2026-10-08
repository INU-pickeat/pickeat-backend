package com.pickeat.pickeatbackend.domain.pick.repository;

import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import java.time.Instant;

public record RestaurantPickSummary(
        Long restaurantId,
        String restaurantName,
        FoodCategory foodCategory,
        String representativeImageUrl,
        String googlePlaceId,
        Long pickCount,
        Instant latestPickedAt
) {
}
