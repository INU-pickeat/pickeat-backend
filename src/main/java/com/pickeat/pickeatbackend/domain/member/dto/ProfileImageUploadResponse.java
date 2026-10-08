package com.pickeat.pickeatbackend.domain.member.dto;

import com.pickeat.pickeatbackend.domain.review.storage.ReviewImageStorage.PresignedUpload;
import java.time.Instant;

// uploadUrl에 Content-Type 헤더를 그대로 붙여 PUT으로 올린 뒤, imageUrl을 PATCH /api/v1/me의 profileImageUrl로 보낸다.
public record ProfileImageUploadResponse(String uploadUrl, String imageUrl, String contentType, Instant expiresAt) {

    public static ProfileImageUploadResponse of(PresignedUpload upload, String contentType) {
        return new ProfileImageUploadResponse(upload.uploadUrl(), upload.imageUrl(), contentType, upload.expiresAt());
    }
}
