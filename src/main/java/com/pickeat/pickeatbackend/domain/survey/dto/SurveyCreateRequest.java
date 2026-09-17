package com.pickeat.pickeatbackend.domain.survey.dto;

import com.pickeat.pickeatbackend.domain.survey.entity.SurveyCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SurveyCreateRequest(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 1000) String description,
        @Size(max = 50) String target,
        @NotNull SurveyCategory category,
        @Positive Integer estimatedMinutes,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
