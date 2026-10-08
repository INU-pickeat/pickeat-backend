package com.pickeat.pickeatbackend.domain.restaurant.repository;

import com.pickeat.pickeatbackend.domain.restaurant.entity.RestaurantReport;
import com.pickeat.pickeatbackend.domain.restaurant.entity.RestaurantReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantReportRepository extends JpaRepository<RestaurantReport, Long> {

    boolean existsByMemberIdAndRestaurantIdAndStatus(Long memberId, Long restaurantId, RestaurantReportStatus status);
}
