package com.pickeat.pickeatbackend.domain.restaurant.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import org.junit.jupiter.api.Test;

class RestaurantNavigationLinksResponseTest {

    @Test
    void 좌표를_지도_링크에_포함한다() {
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
    void 이름에_공백과_쉼표가_있어도_경로에_안전하게_인코딩된다() {
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
