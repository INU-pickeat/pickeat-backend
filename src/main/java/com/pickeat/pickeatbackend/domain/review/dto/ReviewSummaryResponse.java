package com.pickeat.pickeatbackend.domain.review.dto;

import com.pickeat.pickeatbackend.domain.review.entity.ReviewKeyword;
import java.util.List;

// 공개 후기만 집계한다. 후기가 없으면 oneLineReview는 "후기가 없습니다."이고 topKeywords는 빈 배열이다.
public record ReviewSummaryResponse(
        Long restaurantId,
        long reviewCount,
        String oneLineReview,
        // 많이 고른 키워드 상위 5개. 건수 내림차순, 같으면 enum 선언 순서다.
        List<KeywordCount> topKeywords
) {
    public record KeywordCount(String code, String label, long count) {

        public static KeywordCount of(ReviewKeyword keyword, long count) {
            return new KeywordCount(keyword.name(), keyword.getLabel(), count);
        }
    }
}
