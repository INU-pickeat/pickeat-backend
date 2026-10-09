package com.pickeat.pickeatbackend.domain.review.dto;

import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.review.entity.ReviewVisibility;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateReviewRequest(
        @NotNull Long pickId,
        @NotNull @Min(1) @Max(5) Integer rating,
        @NotBlank @Size(max = 1000) String content,
        // 후기 작성 화면에서 사용자가 직접 고른 값이다.
        @NotNull FoodCategory foodCategory,
        @NotNull CompanionType companionType,
        @NotNull ReviewVisibility visibility,
        // 생략하면 이미지 없는 후기다. 업로드 URL API로 올린 뒤 받은 imageUrl만 넣을 수 있다.
        // 최대 5장. 배열 순서가 표시 순서이고 0번이 대표 이미지다.
        @Size(max = MAX_IMAGES) List<@NotBlank @Size(max = 500) String> imageUrls
) {
    public static final int MAX_IMAGES = 5;

    public CreateReviewRequest {
        imageUrls = imageUrls == null ? List.of() : imageUrls;
    }
}
