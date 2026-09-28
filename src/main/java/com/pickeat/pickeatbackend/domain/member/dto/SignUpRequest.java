package com.pickeat.pickeatbackend.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 64) String password,
        @NotBlank @Size(max = 10) @Pattern(regexp = SignUpRequest.NICKNAME_PATTERN) String nickname
) {

    /** 한글 완성형·영문·숫자만 허용한다. 공백·특수문자·이모지·자모는 거부한다. */
    public static final String NICKNAME_PATTERN = "^[가-힣a-zA-Z0-9]+$";

    public SignUpRequest {
        nickname = nickname == null ? null : nickname.strip();
    }
}
