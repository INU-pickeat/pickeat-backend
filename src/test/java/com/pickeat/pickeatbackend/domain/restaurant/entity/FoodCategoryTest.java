package com.pickeat.pickeatbackend.domain.restaurant.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class FoodCategoryTest {

    @Test
    @DisplayName("카테고리로 Google 유형 목록을 역으로 찾는다")
    void resolvesGooglePrimaryTypesFromCategory() {
        Set<String> types = FoodCategory.toGooglePrimaryTypes(Set.of(FoodCategory.KOREAN));

        assertThat(types).containsExactlyInAnyOrder(
                "korean_restaurant", "korean_barbecue_restaurant", "chicken_restaurant",
                "chicken_wings_restaurant", "seafood_restaurant", "noodle_shop");
    }

    @Test
    @DisplayName("여러 카테고리를 합쳐서 Google 유형 목록을 찾는다")
    void resolvesGooglePrimaryTypesFromMultipleCategories() {
        Set<String> types = FoodCategory.toGooglePrimaryTypes(Set.of(FoodCategory.CHINESE, FoodCategory.CAFE_DESSERT));

        assertThat(types).containsExactlyInAnyOrder(
                "chinese_restaurant", "cantonese_restaurant", "chinese_noodle_restaurant",
                "dim_sum_restaurant", "dumpling_restaurant", "hot_pot_restaurant", "taiwanese_restaurant",
                "cafe", "dessert_shop");
    }

    @ParameterizedTest
    @DisplayName("매핑된 Google 유형은 해당 카테고리로 변환된다")
    @CsvSource({
            "korean_restaurant, KOREAN",
            "korean_barbecue_restaurant, KOREAN",
            "chicken_restaurant, KOREAN",
            "chicken_wings_restaurant, KOREAN",
            "seafood_restaurant, KOREAN",
            "noodle_shop, KOREAN",
            "japanese_restaurant, JAPANESE",
            "ramen_restaurant, JAPANESE",
            "japanese_izakaya_restaurant, JAPANESE",
            "sushi_restaurant, JAPANESE",
            "tonkatsu_restaurant, JAPANESE",
            "chinese_restaurant, CHINESE",
            "western_restaurant, WESTERN",
            "italian_restaurant, WESTERN",
            "european_restaurant, WESTERN",
            "sandwich_shop, WESTERN",
            "brunch_restaurant, WESTERN",
            "fast_food_restaurant, WESTERN",
            "hamburger_restaurant, WESTERN",
            "pizza_restaurant, WESTERN",
            "cafe, CAFE_DESSERT",
            "dessert_shop, CAFE_DESSERT",
            "irish_pub, PUB_BAR",
            "cocktail_bar, PUB_BAR",
            "sports_bar, PUB_BAR",
            "indian_restaurant, OTHER",
            "mexican_restaurant, OTHER",
            "vietnamese_restaurant, OTHER"
    })
    void mapsGooglePrimaryTypeToFoodCategory(String primaryType, FoodCategory expected) {
        assertThat(FoodCategory.fromGooglePrimaryType(primaryType)).contains(expected);
    }

    @ParameterizedTest
    @DisplayName("매핑되지 않는 유형은 제외된다")
    @ValueSource(strings = {
            "restaurant",
            "hotel",
            "halal_restaurant",
            "vegan_restaurant",
            "buffet_restaurant",
            "fine_dining_restaurant",
            "meal_takeaway"
    })
    void excludesUnmappedGooglePrimaryType(String primaryType) {
        assertThat(FoodCategory.fromGooglePrimaryType(primaryType)).isEqualTo(Optional.empty());
    }

    @ParameterizedTest
    @DisplayName("null이나 빈 문자열도 제외된다")
    @NullAndEmptySource
    void excludesNullOrBlankPrimaryType(String primaryType) {
        assertThat(FoodCategory.fromGooglePrimaryType(primaryType)).isEqualTo(Optional.empty());
    }
}
