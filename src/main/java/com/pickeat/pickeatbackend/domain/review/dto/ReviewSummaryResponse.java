package com.pickeat.pickeatbackend.domain.review.dto;

// 공개 후기만 집계한다. 후기가 없으면 averageRating은 null, oneLineReview는 "후기가 없습니다."다.
public record ReviewSummaryResponse(
        Long restaurantId,
        long reviewCount,
        Double averageRating,
        String oneLineReview
) {
}
