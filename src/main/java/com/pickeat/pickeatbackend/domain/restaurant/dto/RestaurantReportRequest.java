package com.pickeat.pickeatbackend.domain.restaurant.dto;

import com.pickeat.pickeatbackend.domain.restaurant.entity.RestaurantReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// reason: CLOSED(폐업) · WRONG_INFO(영업시간·주소 등 정보 오류) · WRONG_CATEGORY(음식 종류 오류) · OTHER
// detail은 선택, 최대 300자.
public record RestaurantReportRequest(
        @NotNull RestaurantReportReason reason,
        @Size(max = 300) String detail
) {
}
