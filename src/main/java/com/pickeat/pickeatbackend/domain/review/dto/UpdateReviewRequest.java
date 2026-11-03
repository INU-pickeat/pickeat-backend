package com.pickeat.pickeatbackend.domain.review.dto;

import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.review.entity.ReviewKeyword;
import com.pickeat.pickeatbackend.domain.review.entity.ReviewVisibility;
import com.pickeat.pickeatbackend.domain.review.entity.SpendPerPerson;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

// 부분 수정. 보내지 않은(null) 필드는 유지하며, 작성 시 등록한 사진은 수정할 수 없다.
public record UpdateReviewRequest(
        // 빈 문자열("")이나 공백을 보내면 본문을 지운다(null로 저장).
        @Size(max = 1000) String content,
        FoodCategory foodCategory,
        CompanionType companionType,
        ReviewVisibility visibility,
        // 구버전 클라이언트가 보내도 조용히 무시하지 않고 400 REVIEW_007로 거부한다(ReviewService.update).
        @Schema(hidden = true) Object imageUrls,
        // 보내면 통째로 교체한다(최대 5개). 빈 배열 허용 여부는 ReviewService.KEYWORDS_REQUIRED(지금은 허용).
        @Size(max = CreateReviewRequest.MAX_KEYWORDS) Set<@NotNull ReviewKeyword> keywords,
        SpendPerPerson spendPerPerson
) {
}
