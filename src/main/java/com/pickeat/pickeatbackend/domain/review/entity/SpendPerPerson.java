package com.pickeat.pickeatbackend.domain.review.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 후기 작성자가 고른 1인당 지출 구간. 선택 입력이다.
@Getter
@RequiredArgsConstructor
public enum SpendPerPerson {
    UNDER_10000("1만원 미만"),
    FROM_10000_TO_20000("1~2만원"),
    FROM_20000_TO_30000("2~3만원"),
    OVER_30000("3만원 이상");

    private final String label;
}
