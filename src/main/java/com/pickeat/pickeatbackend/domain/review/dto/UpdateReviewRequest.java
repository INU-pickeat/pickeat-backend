package com.pickeat.pickeatbackend.domain.review.dto;

import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.review.entity.ReviewVisibility;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

// 부분 수정. 보내지 않은(null) 필드는 유지한다. imageUrls는 빈 배열이면 이미지를 전부 지운다.
public record UpdateReviewRequest(
        @Size(max = 1000) String content,
        FoodCategory foodCategory,
        CompanionType companionType,
        ReviewVisibility visibility,
        @Size(max = CreateReviewRequest.MAX_IMAGES) List<@NotBlank @Size(max = 500) String> imageUrls
) {
    @AssertTrue(message = "content는 공백만으로 채울 수 없습니다.")
    public boolean isContentNotBlankWhenPresent() {
        return content == null || !content.isBlank();
    }
}
