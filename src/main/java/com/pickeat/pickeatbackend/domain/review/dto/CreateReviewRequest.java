package com.pickeat.pickeatbackend.domain.review.dto;

import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.review.entity.ReviewKeyword;
import com.pickeat.pickeatbackend.domain.review.entity.ReviewVisibility;
import com.pickeat.pickeatbackend.domain.review.entity.SpendPerPerson;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

public record CreateReviewRequest(
        @NotNull Long pickId,
        // 선택 입력(2026-11-03). 빈 문자열·공백은 본문 없음(null)으로 저장한다.
        @Size(max = 1000) String content,
        // 후기 작성 화면에서 사용자가 직접 고른 값이다.
        @NotNull FoodCategory foodCategory,
        @NotNull CompanionType companionType,
        @NotNull ReviewVisibility visibility,
        // 1~5장 필수. 누락·null·빈 배열은 빈 목록으로 바꿔 ReviewService에서 REVIEW_008로 거부한다.
        // 업로드 URL API로 올린 뒤 받은 imageUrl만 넣을 수 있다. 배열 순서가 표시 순서이고 0번이 대표 이미지다.
        @Size(max = MAX_IMAGES) List<@NotBlank @Size(max = 500) String> imageUrls,
        // 최대 5개. Set이라 중복은 하나로 합쳐진다. 필수 여부는 ReviewService.KEYWORDS_REQUIRED(지금은 선택).
        @Size(max = MAX_KEYWORDS) Set<@NotNull ReviewKeyword> keywords,
        SpendPerPerson spendPerPerson
) {
    public static final int MAX_IMAGES = 5;
    public static final int MAX_KEYWORDS = 5;

    public CreateReviewRequest {
        imageUrls = imageUrls == null ? List.of() : imageUrls;
        keywords = keywords == null ? Set.of() : keywords;
    }
}
