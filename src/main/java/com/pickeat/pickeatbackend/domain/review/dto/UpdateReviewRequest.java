package com.pickeat.pickeatbackend.domain.review.dto;

import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.review.entity.ReviewVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

// 부분 수정. 보내지 않은(null) 필드는 유지하며, 작성 시 등록한 사진은 수정할 수 없다.
public record UpdateReviewRequest(
        @Size(max = 1000) String content,
        FoodCategory foodCategory,
        CompanionType companionType,
        ReviewVisibility visibility,
        // 구버전 클라이언트가 보내도 조용히 무시하지 않고 400으로 거부한다.
        @Schema(hidden = true) @Null Object imageUrls
) {
    @AssertTrue(message = "content는 공백만으로 채울 수 없습니다.")
    public boolean isContentNotBlankWhenPresent() {
        return content == null || !content.isBlank();
    }
}
