package com.pickeat.pickeatbackend.domain.restaurant.entity;

import java.util.Map;
import java.util.Optional;

public enum FoodCategory {
    KOREAN,
    JAPANESE,
    CHINESE,
    WESTERN,
    CAFE_DESSERT;

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
            Map.entry("chinese_restaurant", CHINESE),
            Map.entry("western_restaurant", WESTERN),
            Map.entry("italian_restaurant", WESTERN),
            Map.entry("european_restaurant", WESTERN),
            Map.entry("sandwich_shop", WESTERN),
            Map.entry("irish_pub", WESTERN),
            Map.entry("brunch_restaurant", WESTERN),
            Map.entry("cafe", CAFE_DESSERT),
            Map.entry("dessert_shop", CAFE_DESSERT)
    );

    public static Optional<FoodCategory> fromGooglePrimaryType(String primaryType) {
        if (primaryType == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(GOOGLE_PRIMARY_TYPE_MAPPING.get(primaryType));
    }
}
