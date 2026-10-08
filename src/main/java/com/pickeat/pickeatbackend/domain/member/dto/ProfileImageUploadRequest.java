package com.pickeat.pickeatbackend.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

// 올릴 프로필 이미지의 Content-Type. image/jpeg · image/png · image/webp
public record ProfileImageUploadRequest(@NotBlank String contentType) {
}
