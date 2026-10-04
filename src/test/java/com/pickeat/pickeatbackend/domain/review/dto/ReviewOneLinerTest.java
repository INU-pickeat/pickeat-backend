package com.pickeat.pickeatbackend.domain.review.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewOneLinerTest {

    @Test
    @DisplayName("후기 본문의 첫 줄을 한줄평으로 쓴다")
    void usesFirstLine() {
        assertThat(ReviewOneLiner.from("  양고기가 부드러워요\n다음에 또 올게요")).isEqualTo("양고기가 부드러워요");
    }

    @Test
    @DisplayName("50자를 넘으면 잘라서 말줄임표를 붙인다")
    void truncatesLongLine() {
        String longLine = "가".repeat(60);

        String oneLiner = ReviewOneLiner.from(longLine);

        assertThat(oneLiner).isEqualTo("가".repeat(50) + "…");
    }

    @Test
    @DisplayName("본문이 없으면 '후기가 없습니다.'를 돌려준다")
    void fallsBackWhenBlank() {
        assertThat(ReviewOneLiner.from(null)).isEqualTo("후기가 없습니다.");
        assertThat(ReviewOneLiner.from("   ")).isEqualTo(ReviewOneLiner.NO_REVIEW);
    }
}
