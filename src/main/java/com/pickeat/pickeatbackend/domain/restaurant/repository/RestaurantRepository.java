package com.pickeat.pickeatbackend.domain.restaurant.repository;

import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
}
