package com.pickeat.pickeatbackend.domain.restaurant.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.restaurant.service.FoodCategoryNameOverride.Categories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class FoodCategoryNameOverrideTest {

    @ParameterizedTest
    @CsvSource({
            "더샤브칼국수, KOREAN",
            "송도장어촌, KOREAN",
            "숯불구이장어명가, KOREAN",
            "원조 간장게장, KOREAN",
            "봉추찜닭 홍대점, KOREAN",
            "양평해장국 인천대점, KOREAN",
            "소래막회, KOREAN",
            "명동 왕만두, KOREAN",
            "쑝쑝돈까스 부천 원미점, KOREAN",
            "호우양꼬치, CHINESE",
            "라화쿵부 마라탕, CHINESE",
            "홍콩 딤섬, CHINESE",
            "오빠초밥, JAPANESE",
            "스시소요, JAPANESE",
            "오레노라멘 본점, JAPANESE",
            "카츠혼 돈카츠, JAPANESE",
            "롤링파스타 연남점, WESTERN",
            "연남 브런치카페, WESTERN",
            "아키비스트 커피, CAFE_DESSERT",
            "역전할머니맥주, PUB_BAR",
            "한신포차 1호점, PUB_BAR",
            "에머이 쌀국수, OTHER"
    })
    @DisplayName("이름에 키워드가 있으면 그 카테고리로 보정한다")
    void overridesCategoryByKeyword(String name, FoodCategory expected) {
        assertThat(FoodCategoryNameOverride.find(name)).map(Categories::primary).contains(expected);
    }

    @Test
    @DisplayName("여러 키워드가 걸리면 가장 긴 키워드를 따른다")
    void prefersLongestKeyword() {
        // 쌀국수(기타) > 국수(한식), 타코야끼(일식) > 타코(기타), 돈카츠(일식)와 돈까스(한식)는 서로 다른 키워드
        assertThat(FoodCategoryNameOverride.find("하노이 쌀국수")).map(Categories::primary)
                .contains(FoodCategory.OTHER);
        assertThat(FoodCategoryNameOverride.find("오사카 타코야끼")).map(Categories::primary)
                .contains(FoodCategory.JAPANESE);
        assertThat(FoodCategoryNameOverride.find("멕시칸 타코")).map(Categories::primary)
                .contains(FoodCategory.OTHER);
    }

    @Test
    @DisplayName("치킨과 이자카야는 주점을 보조 카테고리로 함께 돌려준다")
    void returnsSecondaryCategoryForDualKeywords() {
        assertThat(FoodCategoryNameOverride.find("교촌치킨 연남점"))
                .contains(new Categories(FoodCategory.KOREAN, FoodCategory.PUB_BAR));
        assertThat(FoodCategoryNameOverride.find("갓포 이자카야 오가"))
                .contains(new Categories(FoodCategory.JAPANESE, FoodCategory.PUB_BAR));
        assertThat(FoodCategoryNameOverride.find("더샤브칼국수")).map(Categories::secondary).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"예당샤브샤브", "정든가츠", "맛있는 식당", "헤키", "공감 홍대점", "오리지널 키친", "죽전 식당"})
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
