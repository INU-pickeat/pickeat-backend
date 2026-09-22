package com.pickeat.pickeatbackend.domain.restaurant.entity;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public enum FoodCategory {
    KOREAN,
    JAPANESE,
    CHINESE,
    WESTERN,
    CAFE_DESSERT,
    PUB_BAR,
    OTHER;

    // 연남·한남·서촌·신사·논현 100개 표본 검증(2026-09-20) 기준 매핑. 표는 docs/SERVICE_DECISIONS.md와 동일하게 유지한다.
    private static final Map<String, FoodCategory> GOOGLE_PRIMARY_TYPE_MAPPING = Map.ofEntries(
            Map.entry("korean_restaurant", KOREAN),
            Map.entry("korean_barbecue_restaurant", KOREAN),
            Map.entry("chicken_restaurant", KOREAN),
            Map.entry("chicken_wings_restaurant", KOREAN),
            Map.entry("seafood_restaurant", KOREAN),
            Map.entry("noodle_shop", KOREAN),
            Map.entry("japanese_restaurant", JAPANESE),
            Map.entry("ramen_restaurant", JAPANESE),
            Map.entry("japanese_curry_restaurant", JAPANESE),
            Map.entry("japanese_izakaya_restaurant", JAPANESE),
            Map.entry("sushi_restaurant", JAPANESE),
            Map.entry("tonkatsu_restaurant", JAPANESE),
            Map.entry("yakiniku_restaurant", JAPANESE),
            Map.entry("yakitori_restaurant", JAPANESE),
            Map.entry("chinese_restaurant", CHINESE),
            Map.entry("cantonese_restaurant", CHINESE),
            Map.entry("chinese_noodle_restaurant", CHINESE),
            Map.entry("dim_sum_restaurant", CHINESE),
            Map.entry("dumpling_restaurant", CHINESE),
            Map.entry("hot_pot_restaurant", CHINESE),
            Map.entry("taiwanese_restaurant", CHINESE),
            Map.entry("western_restaurant", WESTERN),
            Map.entry("italian_restaurant", WESTERN),
            Map.entry("european_restaurant", WESTERN),
            Map.entry("sandwich_shop", WESTERN),
            Map.entry("brunch_restaurant", WESTERN),
            Map.entry("fast_food_restaurant", WESTERN),
            Map.entry("hamburger_restaurant", WESTERN),
            Map.entry("pizza_restaurant", WESTERN),
            Map.entry("cafe", CAFE_DESSERT),
            Map.entry("dessert_shop", CAFE_DESSERT),
            Map.entry("bar", PUB_BAR),
            Map.entry("pub", PUB_BAR),
            Map.entry("wine_bar", PUB_BAR),
            Map.entry("cocktail_bar", PUB_BAR),
            Map.entry("sports_bar", PUB_BAR),
            Map.entry("gastropub", PUB_BAR),
            Map.entry("brewpub", PUB_BAR),
            Map.entry("brewery", PUB_BAR),
            Map.entry("lounge_bar", PUB_BAR),
            Map.entry("hookah_bar", PUB_BAR),
            Map.entry("irish_pub", PUB_BAR),
            Map.entry("beer_garden", PUB_BAR),
            Map.entry("bar_and_grill", PUB_BAR),
            Map.entry("afghani_restaurant", OTHER),
            Map.entry("african_restaurant", OTHER),
            Map.entry("american_restaurant", OTHER),
            Map.entry("argentinian_restaurant", OTHER),
            Map.entry("asian_fusion_restaurant", OTHER),
            Map.entry("asian_restaurant", OTHER),
            Map.entry("australian_restaurant", OTHER),
            Map.entry("austrian_restaurant", OTHER),
            Map.entry("bangladeshi_restaurant", OTHER),
            Map.entry("barbecue_restaurant", OTHER),
            Map.entry("basque_restaurant", OTHER),
            Map.entry("bavarian_restaurant", OTHER),
            Map.entry("belgian_restaurant", OTHER),
            Map.entry("brazilian_restaurant", OTHER),
            Map.entry("british_restaurant", OTHER),
            Map.entry("burmese_restaurant", OTHER),
            Map.entry("cajun_restaurant", OTHER),
            Map.entry("cambodian_restaurant", OTHER),
            Map.entry("caribbean_restaurant", OTHER),
            Map.entry("chilean_restaurant", OTHER),
            Map.entry("colombian_restaurant", OTHER),
            Map.entry("croatian_restaurant", OTHER),
            Map.entry("cuban_restaurant", OTHER),
            Map.entry("czech_restaurant", OTHER),
            Map.entry("danish_restaurant", OTHER),
            Map.entry("dutch_restaurant", OTHER),
            Map.entry("eastern_european_restaurant", OTHER),
            Map.entry("ethiopian_restaurant", OTHER),
            Map.entry("filipino_restaurant", OTHER),
            Map.entry("french_restaurant", OTHER),
            Map.entry("fusion_restaurant", OTHER),
            Map.entry("german_restaurant", OTHER),
            Map.entry("greek_restaurant", OTHER),
            Map.entry("hawaiian_restaurant", OTHER),
            Map.entry("indian_restaurant", OTHER),
            Map.entry("indonesian_restaurant", OTHER),
            Map.entry("irish_restaurant", OTHER),
            Map.entry("israeli_restaurant", OTHER),
            Map.entry("latin_american_restaurant", OTHER),
            Map.entry("lebanese_restaurant", OTHER),
            Map.entry("malaysian_restaurant", OTHER),
            Map.entry("mediterranean_restaurant", OTHER),
            Map.entry("mexican_restaurant", OTHER),
            Map.entry("middle_eastern_restaurant", OTHER),
            Map.entry("mongolian_barbecue_restaurant", OTHER),
            Map.entry("moroccan_restaurant", OTHER),
            Map.entry("pakistani_restaurant", OTHER),
            Map.entry("persian_restaurant", OTHER),
            Map.entry("peruvian_restaurant", OTHER),
            Map.entry("polish_restaurant", OTHER),
            Map.entry("portuguese_restaurant", OTHER),
            Map.entry("romanian_restaurant", OTHER),
            Map.entry("russian_restaurant", OTHER),
            Map.entry("scandinavian_restaurant", OTHER),
            Map.entry("south_american_restaurant", OTHER),
            Map.entry("south_indian_restaurant", OTHER),
            Map.entry("spanish_restaurant", OTHER),
            Map.entry("sri_lankan_restaurant", OTHER),
            Map.entry("steak_house", OTHER),
            Map.entry("swiss_restaurant", OTHER),
            Map.entry("thai_restaurant", OTHER),
            Map.entry("tibetan_restaurant", OTHER),
            Map.entry("turkish_restaurant", OTHER),
            Map.entry("ukrainian_restaurant", OTHER),
            Map.entry("vietnamese_restaurant", OTHER)
    );

    public static Optional<FoodCategory> fromGooglePrimaryType(String primaryType) {
        if (primaryType == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(GOOGLE_PRIMARY_TYPE_MAPPING.get(primaryType));
    }

    // Google Nearby Search의 includedPrimaryTypes로 넘겨 주 유형 기준으로 선처리시킨다.
    public static Set<String> toGooglePrimaryTypes(Set<FoodCategory> categories) {
        return GOOGLE_PRIMARY_TYPE_MAPPING.entrySet().stream()
                .filter(entry -> categories.contains(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }
}
