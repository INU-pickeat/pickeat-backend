package com.pickeat.pickeatbackend.domain.pick.dto;

import com.pickeat.pickeatbackend.domain.pick.entity.PickStatus;
import jakarta.validation.constraints.NotNull;

public record PickStatusUpdateRequest(@NotNull PickStatus status) {
}
