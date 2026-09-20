package com.pickeat.pickeatbackend.domain.restaurant.repository;

import java.util.List;

public interface RestaurantRepositoryCustom {

    List<RestaurantCandidate> findWithinRadius(double latitude, double longitude, double radiusMeters);
}
