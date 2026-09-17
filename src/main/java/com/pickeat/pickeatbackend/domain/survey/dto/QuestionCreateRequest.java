package com.pickeat.pickeatbackend.domain.survey.dto;

import com.pickeat.pickeatbackend.domain.survey.entity.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record QuestionCreateRequest(
        @NotNull QuestionType type,
        @NotBlank @Size(max = 200) String content,
        boolean isRequired,
        boolean allowMultiple,
        List<@NotBlank @Size(max = 200) String> options
) {
}
