package com.pickeat.pickeatbackend.domain.survey.dto;

import com.pickeat.pickeatbackend.domain.survey.entity.QuestionOption;

public record QuestionOptionResponse(Long id, String content) {

    public static QuestionOptionResponse from(QuestionOption option) {
        return new QuestionOptionResponse(option.getId(), option.getContent());
    }
}
