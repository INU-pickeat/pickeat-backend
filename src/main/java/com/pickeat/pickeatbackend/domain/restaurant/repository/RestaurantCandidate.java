package com.pickeat.pickeatbackend.domain.restaurant.repository;

import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;

public record RestaurantCandidate(Restaurant restaurant, double distanceMeters) {
}
