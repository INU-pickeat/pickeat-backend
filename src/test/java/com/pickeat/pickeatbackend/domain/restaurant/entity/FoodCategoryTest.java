package com.pickeat.pickeatbackend.domain.restaurant.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class FoodCategoryTest {

    @ParameterizedTest
    @CsvSource({
            "korean_restaurant, KOREAN",
            "korean_barbecue_restaurant, KOREAN",
            "chicken_restaurant, KOREAN",
            "chicken_wings_restaurant, KOREAN",
            "seafood_restaurant, KOREAN",
            "noodle_shop, KOREAN",
            "japanese_restaurant, JAPANESE",
            "ramen_restaurant, JAPANESE",
            "chinese_restaurant, CHINESE",
            "western_restaurant, WESTERN",
            "italian_restaurant, WESTERN",
            "european_restaurant, WESTERN",
            "sandwich_shop, WESTERN",
            "irish_pub, WESTERN",
            "brunch_restaurant, WESTERN",
            "cafe, CAFE_DESSERT",
            "dessert_shop, CAFE_DESSERT"
    })
    void 매핑된_Google_유형은_해당_카테고리로_변환된다(String primaryType, FoodCategory expected) {
        assertThat(FoodCategory.fromGooglePrimaryType(primaryType)).contains(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "restaurant",
            "hotel",
            "halal_restaurant",
            "vegan_restaurant",
            "buffet_restaurant",
            "fine_dining_restaurant",
            "indian_restaurant",
            "turkish_restaurant",
            "asian_fusion_restaurant",
            "meal_takeaway"
    })
    void 매핑되지_않는_유형은_제외된다(String primaryType) {
        assertThat(FoodCategory.fromGooglePrimaryType(primaryType)).isEqualTo(Optional.empty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void null이나_빈_문자열도_제외된다(String primaryType) {
        assertThat(FoodCategory.fromGooglePrimaryType(primaryType)).isEqualTo(Optional.empty());
    }
}
