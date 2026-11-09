package com.pickeat.pickeatbackend.domain.restaurant.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.restaurant.entity.DataProvider;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestaurantResponseTest {

    private Restaurant restaurant(DataProvider dataProvider) {
        return Restaurant.builder()
                .name("테스트 식당").googlePlaceId("place-1").latitude(37.5).longitude(127.0)
                .dataProvider(dataProvider)
                .build();
    }

    @Test
    @DisplayName("Google에서 받은 식당은 출처로 Google Maps를 내려준다")
    void attributesGoogleRestaurantToGoogleMaps() {
        assertThat(RestaurantResponse.from(restaurant(DataProvider.GOOGLE), null).dataAttribution())
                .isEqualTo("Google Maps");
    }

    @Test
    @DisplayName("운영자가 직접 넣은 식당은 출처가 없다")
    void hasNoAttributionForCuratedRestaurant() {
        assertThat(RestaurantResponse.from(restaurant(DataProvider.CURATED), null).dataAttribution()).isNull();
    }
}
