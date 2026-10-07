package com.pickeat.pickeatbackend.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Locale;

public record EmailVerificationRequest(@NotBlank @Email String email) {

    public EmailVerificationRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
