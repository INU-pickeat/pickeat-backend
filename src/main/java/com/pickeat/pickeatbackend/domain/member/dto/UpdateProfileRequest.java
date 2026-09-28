package com.pickeat.pickeatbackend.domain.member.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 생략(null)한 필드는 변경하지 않는다. bio·profileImageUrl은 빈 문자열이면 삭제한다.
 */
public record UpdateProfileRequest(
        @Size(max = 10) @Pattern(regexp = SignUpRequest.NICKNAME_PATTERN) String nickname,
        @Size(max = 150) String bio,
        @Size(max = 500) @Pattern(regexp = "^$|^https?://\\S+$") String profileImageUrl
) {

    public UpdateProfileRequest {
        nickname = nickname == null ? null : nickname.strip();
    }
}
