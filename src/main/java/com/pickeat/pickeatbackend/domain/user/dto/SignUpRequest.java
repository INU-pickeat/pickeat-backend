package com.pickeat.pickeatbackend.domain.user.dto;

import com.pickeat.pickeatbackend.domain.user.entity.Gender;
import com.pickeat.pickeatbackend.domain.user.entity.Job;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 64) String password,
        @NotNull Gender gender,
        @NotNull @Positive Integer age,
        @NotNull Job job
) {
}
