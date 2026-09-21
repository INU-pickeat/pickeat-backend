package com.pickeat.pickeatbackend.domain.restaurant.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestaurantTest {

    @Test
    @DisplayName("공급자를 지정하지 않으면 GOOGLE로 초기화된다")
    void defaultsToGoogleWhenProviderNotSpecified() {
        Restaurant restaurant = Restaurant.builder()
                .name("픽잇 식당")
                .latitude(37.58)
                .longitude(127.0)
                .build();

        assertThat(restaurant.getDataProvider()).isEqualTo(DataProvider.GOOGLE);
    }

    @Test
    @DisplayName("동행 적합도는 미확인 NULL을 유지한다")
    void keepsCompanionSuitabilityNullWhenUnset() {
        Restaurant restaurant = Restaurant.builder()
                .name("픽잇 식당")
                .latitude(37.58)
                .longitude(127.0)
                .build();

        assertThat(restaurant.getSuitableForDate()).isNull();
        assertThat(restaurant.getSuitableForSolo()).isNull();
    }
}
