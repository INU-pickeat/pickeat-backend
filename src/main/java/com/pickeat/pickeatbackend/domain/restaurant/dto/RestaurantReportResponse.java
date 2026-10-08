package com.pickeat.pickeatbackend.domain.restaurant.dto;

import com.pickeat.pickeatbackend.domain.restaurant.entity.RestaurantReport;
import com.pickeat.pickeatbackend.domain.restaurant.entity.RestaurantReportReason;
import com.pickeat.pickeatbackend.domain.restaurant.entity.RestaurantReportStatus;
import java.time.Instant;

public record RestaurantReportResponse(
        Long reportId,
        Long restaurantId,
        RestaurantReportReason reason,
        RestaurantReportStatus status,
        Instant createdAt
) {
    public static RestaurantReportResponse from(RestaurantReport report) {
        return new RestaurantReportResponse(
                report.getId(), report.getRestaurantId(), report.getReason(), report.getStatus(), report.getCreatedAt());
    }
}
