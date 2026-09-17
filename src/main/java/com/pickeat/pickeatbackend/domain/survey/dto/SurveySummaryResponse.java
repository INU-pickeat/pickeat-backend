package com.pickeat.pickeatbackend.domain.survey.dto;

import com.pickeat.pickeatbackend.domain.survey.entity.Survey;
import com.pickeat.pickeatbackend.domain.survey.entity.SurveyCategory;
import java.time.LocalDate;

public record SurveySummaryResponse(
        Long id,
        Long creatorId,
        String title,
        SurveyCategory category,
        LocalDate startDate,
        LocalDate endDate
) {

    public static SurveySummaryResponse from(Survey survey) {
        return new SurveySummaryResponse(
                survey.getId(),
                survey.getCreator().getId(),
                survey.getTitle(),
                survey.getCategory(),
                survey.getStartDate(),
                survey.getEndDate()
        );
    }
}
