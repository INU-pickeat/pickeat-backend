package com.pickeat.pickeatbackend.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CompanionNameHintTest {

    @ParameterizedTest
    @CsvSource({
            "롤링파스타 연남점, DATE, true",
            "스시소요, DATE, true",
            "연남 와인바, DATE, true",
            "양평해장국 인천대점, DATE, false",
            "마왕족발, DATE, false",
            "오레노라멘 본점, SOLO, true",
            "칸지돈부리 덮밥, SOLO, true",
            "양평해장국 국밥, SOLO, true",
            "하남돼지집 삼겹살, SOLO, false",
            "스시 오마카세 진, SOLO, false"
    })
    @DisplayName("메뉴 키워드로 동행 적합도를 추정한다")
    void estimatesSuitabilityFromMenuKeyword(String name, CompanionType companionType, boolean expected) {
        assertThat(CompanionNameHint.find(name, companionType)).contains(expected);
    }

    @Test
    @DisplayName("키워드가 겹치면 가장 긴 것을, 길이가 같으면 이름에서 뒤에 나오는 것을 따른다")
    void resolvesOverlappingKeywords() {
        // 갈비탕(혼밥 적합)이 갈비(혼밥 부적합)보다 길다
        assertThat(CompanionNameHint.find("본가 갈비탕", CompanionType.SOLO)).contains(true);
        // 곱창(부적합)과 국밥(적합)은 길이가 같고, 주메뉴인 국밥이 뒤에 온다
        assertThat(CompanionNameHint.find("황소곱창국밥", CompanionType.SOLO)).contains(true);
        assertThat(CompanionNameHint.find("신촌 갈비 곱창", CompanionType.SOLO)).contains(false);
    }

    @Test
    @DisplayName("키워드가 없거나 다루지 않는 동행 유형이면 모름이다")
    void returnsEmptyWhenUnknown() {
        assertThat(CompanionNameHint.find("헤키", CompanionType.DATE)).isEmpty();
        assertThat(CompanionNameHint.find("롤링파스타 연남점", CompanionType.FAMILY)).isEmpty();
        assertThat(CompanionNameHint.find(null, CompanionType.DATE)).isEmpty();
        assertThat(CompanionNameHint.find("  ", CompanionType.SOLO)).isEmpty();
    }
}
