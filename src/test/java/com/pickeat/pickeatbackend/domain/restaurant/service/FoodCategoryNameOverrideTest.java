package com.pickeat.pickeatbackend.domain.restaurant.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class FoodCategoryNameOverrideTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "더샤브칼국수",
            "송도장어촌",
            "숯불구이장어명가",
            "원조 간장게장",
            "봉추찜닭 홍대점",
            "양평해장국 인천대점",
            "춘천 닭갈비"
    })
    @DisplayName("한식 키워드가 이름에 있으면 한식으로 보정한다")
    void overridesToKorean(String name) {
        assertThat(FoodCategoryNameOverride.find(name)).contains(FoodCategory.KOREAN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"호우양꼬치", "경성 양꼬치 연남점", "라화쿵부 마라탕"})
    @DisplayName("중식 키워드가 이름에 있으면 중식으로 보정한다")
    void overridesToChinese(String name) {
        assertThat(FoodCategoryNameOverride.find(name)).contains(FoodCategory.CHINESE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"오레노라멘 본점", "스시소요", "예당샤브샤브", "정든가츠", "맛있는 식당"})
    @DisplayName("키워드가 없으면 보정하지 않는다")
    void doesNotOverrideWithoutKeyword(String name) {
        assertThat(FoodCategoryNameOverride.find(name)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("이름이 없으면 보정하지 않는다")
    void doesNotOverrideWithoutName(String name) {
        assertThat(FoodCategoryNameOverride.find(name)).isEmpty();
    }
}
