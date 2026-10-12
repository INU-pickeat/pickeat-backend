package com.pickeat.pickeatbackend.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record PasswordResetRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 64) String newPassword
) {

    public PasswordResetRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
