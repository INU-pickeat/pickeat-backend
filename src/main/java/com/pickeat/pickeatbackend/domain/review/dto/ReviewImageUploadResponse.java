package com.pickeat.pickeatbackend.domain.review.dto;

import com.pickeat.pickeatbackend.domain.review.storage.ReviewImageStorage.PresignedUpload;
import java.time.Instant;
import java.util.List;

public record ReviewImageUploadResponse(List<Upload> uploads) {

    // uploadUrl에 요청한 Content-Type 헤더 그대로 PUT으로 올린 뒤, imageUrl을 후기 작성 요청의 imageUrls에 넣는다.
    public record Upload(String uploadUrl, String imageUrl, String contentType, Instant expiresAt) {
        public static Upload of(PresignedUpload upload, String contentType) {
            return new Upload(upload.uploadUrl(), upload.imageUrl(), contentType, upload.expiresAt());
        }
    }
}
