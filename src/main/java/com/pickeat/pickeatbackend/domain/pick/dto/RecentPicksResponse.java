package com.pickeat.pickeatbackend.domain.pick.dto;

import com.pickeat.pickeatbackend.domain.pick.repository.RestaurantPickSummary;
import com.pickeat.pickeatbackend.domain.restaurant.dto.RestaurantImageUrl;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import java.time.Instant;
import java.util.List;

public record RecentPicksResponse(List<Item> restaurants) {

    public static RecentPicksResponse from(List<RestaurantPickSummary> summaries) {
        return new RecentPicksResponse(summaries.stream().map(Item::from).toList());
    }

    // representativeImageUrl: 자체 이미지가 없는 Google 출처 식당은 식당 사진 API 주소, 둘 다 없으면 null.
    public record Item(
            Long restaurantId,
            String restaurantName,
            FoodCategory foodCategory,
            String representativeImageUrl,
            long pickCount,
            Instant latestPickedAt
    ) {
        public static Item from(RestaurantPickSummary summary) {
            return new Item(
                    summary.restaurantId(),
                    summary.restaurantName(),
                    summary.foodCategory(),
                    RestaurantImageUrl.of(
                            summary.restaurantId(), summary.representativeImageUrl(), summary.googlePlaceId()),
                    summary.pickCount(),
                    summary.latestPickedAt());
        }
    }
}
