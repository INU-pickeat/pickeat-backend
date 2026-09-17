package com.pickeat.pickeatbackend.domain.survey.dto;

import com.pickeat.pickeatbackend.domain.survey.entity.Survey;
import com.pickeat.pickeatbackend.domain.survey.entity.SurveyCategory;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SurveyDetailResponse(
        Long id,
        Long creatorId,
        String title,
        String description,
        String target,
        SurveyCategory category,
        Integer estimatedMinutes,
        LocalDate startDate,
        LocalDate endDate,
        boolean sharedToArchive,
        LocalDateTime createdAt
) {

    public static SurveyDetailResponse from(Survey survey) {
        return new SurveyDetailResponse(
                survey.getId(),
                survey.getCreator().getId(),
                survey.getTitle(),
                survey.getDescription(),
                survey.getTarget(),
                survey.getCategory(),
                survey.getEstimatedMinutes(),
                survey.getStartDate(),
                survey.getEndDate(),
                survey.isSharedToArchive(),
                survey.getCreatedAt()
        );
    }
}
