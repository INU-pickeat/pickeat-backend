package com.pickeat.pickeatbackend.domain.restaurant.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestaurantNavigationLinksResponseTest {

    @Test
    @DisplayName("좌표를 지도 링크에 포함한다")
    void includesCoordinatesInMapLinks() {
        Restaurant restaurant = Restaurant.builder()
                .name("맛있는 식당")
                .latitude(37.5665)
                .longitude(126.978)
                .build();

        RestaurantNavigationLinksResponse response = RestaurantNavigationLinksResponse.from(restaurant);

        assertThat(response.kakaoMapUrl()).contains("37.5665,126.978");
        assertThat(response.naverMapUrl()).contains("c=126.978,37.5665");
    }

    @Test
    @DisplayName("이름에 공백과 쉼표가 있어도 경로에 안전하게 인코딩된다")
    void encodesNameSafelyWhenContainingSpacesAndCommas() {
        Restaurant restaurant = Restaurant.builder()
                .name("맛있는, 식당 2호점")
                .latitude(37.0)
                .longitude(127.0)
                .build();

        RestaurantNavigationLinksResponse response = RestaurantNavigationLinksResponse.from(restaurant);

        assertThat(response.kakaoMapUrl()).doesNotContain(" ").doesNotContain("+");
        assertThat(response.naverMapUrl()).doesNotContain(" ").doesNotContain("+");
        assertThat(response.kakaoMapUrl()).contains("%2C").contains("%20");
    }
}
