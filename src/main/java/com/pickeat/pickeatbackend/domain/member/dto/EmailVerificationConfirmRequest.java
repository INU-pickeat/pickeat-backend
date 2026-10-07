package com.pickeat.pickeatbackend.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Locale;

public record EmailVerificationConfirmRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{6}") String code
) {

    public EmailVerificationConfirmRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
