package com.pickeat.pickeatbackend.domain.review.dto;

import com.pickeat.pickeatbackend.domain.review.entity.ReviewKeyword;
import com.pickeat.pickeatbackend.domain.review.entity.SpendPerPerson;
import java.util.List;

// 후기 키워드·1인당 지출을 프론트가 그대로 그릴 수 있게 enum 코드와 한국어 라벨을 함께 내려준다.
public record CodeLabelResponse(String code, String label) {

    public static CodeLabelResponse from(ReviewKeyword keyword) {
        return new CodeLabelResponse(keyword.name(), keyword.getLabel());
    }

    // 1인당 지출은 선택 입력이라 없으면 null이다.
    public static CodeLabelResponse from(SpendPerPerson spendPerPerson) {
        return spendPerPerson == null ? null : new CodeLabelResponse(spendPerPerson.name(), spendPerPerson.getLabel());
    }

    public static List<CodeLabelResponse> fromKeywords(List<ReviewKeyword> keywords) {
        return keywords.stream().map(CodeLabelResponse::from).toList();
    }
}
