package com.pickeat.pickeatbackend.domain.restaurant.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RestaurantTest {

    @Test
    void 공급자를_지정하지_않으면_GOOGLE로_초기화된다() {
        Restaurant restaurant = Restaurant.builder()
                .name("픽잇 식당")
                .latitude(37.58)
                .longitude(127.0)
                .build();

        assertThat(restaurant.getDataProvider()).isEqualTo(DataProvider.GOOGLE);
    }

    @Test
    void 동행_적합도는_미확인_NULL을_유지한다() {
        Restaurant restaurant = Restaurant.builder()
                .name("픽잇 식당")
                .latitude(37.58)
                .longitude(127.0)
                .build();

        assertThat(restaurant.getSuitableForDate()).isNull();
        assertThat(restaurant.getSuitableForSolo()).isNull();
    }
}
