package com.pickeat.pickeatbackend.domain.response.dto;

import jakarta.validation.Valid;
import java.util.List;

public record SurveyResponseCreateRequest(
        String guestKey,
        @Valid List<AnswerCreateRequest> answers
) {
}
