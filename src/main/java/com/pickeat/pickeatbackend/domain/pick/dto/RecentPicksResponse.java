package com.pickeat.pickeatbackend.domain.pick.dto;

import com.pickeat.pickeatbackend.domain.pick.repository.RestaurantPickSummary;
import java.time.Instant;
import java.util.List;

public record RecentPicksResponse(List<Item> restaurants) {

    public static RecentPicksResponse from(List<RestaurantPickSummary> summaries) {
        return new RecentPicksResponse(summaries.stream().map(Item::from).toList());
    }

    public record Item(Long restaurantId, String restaurantName, long pickCount, Instant latestPickedAt) {
        public static Item from(RestaurantPickSummary summary) {
            return new Item(
                    summary.restaurantId(), summary.restaurantName(), summary.pickCount(), summary.latestPickedAt());
        }
    }
}
