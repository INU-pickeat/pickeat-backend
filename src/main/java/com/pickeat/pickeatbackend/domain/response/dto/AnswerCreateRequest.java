package com.pickeat.pickeatbackend.domain.response.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AnswerCreateRequest(
        @NotNull Long questionId,
        String answerText,
        List<Long> optionIds
) {
}
