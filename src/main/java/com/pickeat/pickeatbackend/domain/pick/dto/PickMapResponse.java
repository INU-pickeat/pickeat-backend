package com.pickeat.pickeatbackend.domain.pick.dto;

import com.pickeat.pickeatbackend.domain.pick.entity.Pick;
import com.pickeat.pickeatbackend.domain.pick.entity.PickStatus;
import java.util.List;

public record PickMapResponse(List<Item> picks) {

    public static PickMapResponse from(List<Pick> picks) {
        return new PickMapResponse(picks.stream().map(Item::from).toList());
    }

    public record Item(
            Long pickId,
            Long restaurantId,
            String restaurantName,
            double latitude,
            double longitude,
            PickStatus status
    ) {
        private static Item from(Pick pick) {
            return new Item(
                    pick.getId(),
                    pick.getRestaurant().getId(),
                    pick.getRestaurant().getName(),
                    pick.getRestaurant().getLatitude(),
                    pick.getRestaurant().getLongitude(),
                    pick.getStatus()
            );
        }
    }
}
