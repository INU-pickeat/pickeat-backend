package com.pickeat.pickeatbackend.domain.pick.dto;

import com.pickeat.pickeatbackend.domain.pick.entity.Pick;
import com.pickeat.pickeatbackend.domain.pick.entity.PickStatus;
import java.time.Instant;

public record PickResponse(
        Long pickId,
        Long restaurantId,
        String restaurantName,
        PickStatus status,
        Instant selectedAt,
        Instant visitedAt
) {
    public static PickResponse from(Pick pick) {
        return new PickResponse(
                pick.getId(),
                pick.getRestaurant().getId(),
                pick.getRestaurant().getName(),
                pick.getStatus(),
                pick.getSelectedAt(),
                pick.getVisitedAt()
        );
    }
}
