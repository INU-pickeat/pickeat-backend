package com.pickeat.pickeatbackend.domain.pick.dto;

import jakarta.validation.constraints.NotNull;

public record CreatePickRequest(
        @NotNull Long recommendationSessionId,
        @NotNull Long restaurantId
) {
}
