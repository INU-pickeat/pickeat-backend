package com.pickeat.pickeatbackend.domain.pick.repository;

import java.time.Instant;

public record RestaurantPickSummary(Long restaurantId, String restaurantName, Long pickCount, Instant latestPickedAt) {
}
