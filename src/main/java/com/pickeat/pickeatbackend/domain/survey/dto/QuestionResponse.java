package com.pickeat.pickeatbackend.domain.survey.dto;

import com.pickeat.pickeatbackend.domain.survey.entity.Question;
import com.pickeat.pickeatbackend.domain.survey.entity.QuestionType;
import java.util.List;

public record QuestionResponse(
        Long id,
        QuestionType type,
        String content,
        boolean isRequired,
        boolean allowMultiple,
        List<QuestionOptionResponse> options
) {

    public static QuestionResponse from(Question question, List<QuestionOptionResponse> options) {
        return new QuestionResponse(
                question.getId(),
                question.getType(),
                question.getContent(),
                question.isRequired(),
                question.isAllowMultiple(),
                options
        );
    }
}
