package com.pickeat.pickeatbackend.domain.pick.dto;

import com.pickeat.pickeatbackend.domain.pick.entity.Pick;
import com.pickeat.pickeatbackend.domain.pick.entity.PickStatus;
import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import java.util.List;
import java.util.Map;

public record PickMapResponse(List<Item> picks) {

    public static PickMapResponse from(List<Pick> picks, Map<Long, Long> reviewIdByPickId) {
        return new PickMapResponse(picks.stream()
                .map(pick -> Item.from(pick, reviewIdByPickId.get(pick.getId())))
                .toList());
    }

    public record Item(
            Long pickId,
            Long reviewId,
            Long restaurantId,
            String restaurantName,
            double latitude,
            double longitude,
            PickStatus status,
            CompanionType companionType
    ) {
        private static Item from(Pick pick, Long reviewId) {
            return new Item(
                    pick.getId(),
                    reviewId,
                    pick.getRestaurant().getId(),
                    pick.getRestaurant().getName(),
                    pick.getRestaurant().getLatitude(),
                    pick.getRestaurant().getLongitude(),
                    pick.getStatus(),
                    pick.getCompanionType()
            );
        }
    }
}
