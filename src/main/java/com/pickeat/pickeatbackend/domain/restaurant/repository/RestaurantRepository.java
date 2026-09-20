package com.pickeat.pickeatbackend.domain.restaurant.repository;

import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long>, RestaurantRepositoryCustom {

    Optional<Restaurant> findByGooglePlaceId(String googlePlaceId);
}
