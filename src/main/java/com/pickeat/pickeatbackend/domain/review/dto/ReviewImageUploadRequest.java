package com.pickeat.pickeatbackend.domain.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

// 올릴 이미지의 Content-Type을 보낸다. 후기 이미지 장수(최대 5장)만큼 받는다. 예: ["image/jpeg", "image/png"]
public record ReviewImageUploadRequest(
        @NotEmpty @Size(max = CreateReviewRequest.MAX_IMAGES) List<@NotBlank String> contentTypes
) {
}
