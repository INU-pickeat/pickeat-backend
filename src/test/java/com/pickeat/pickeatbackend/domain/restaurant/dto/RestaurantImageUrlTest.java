package com.pickeat.pickeatbackend.domain.restaurant.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RestaurantImageUrlTest {

    private Restaurant restaurant(Long id, String googlePlaceId, String representativeImageUrl) {
        Restaurant restaurant = Restaurant.builder()
                .name("테스트 식당")
                .googlePlaceId(googlePlaceId)
                .latitude(37.5)
                .longitude(127.0)
                .build();
        ReflectionTestUtils.setField(restaurant, "id", id);
        ReflectionTestUtils.setField(restaurant, "representativeImageUrl", representativeImageUrl);
        return restaurant;
    }

    @Test
    @DisplayName("자체 이미지가 있으면 그 주소를 쓴다")
    void prefersOwnImage() {
        assertThat(RestaurantImageUrl.of(restaurant(1L, "place-1", "/images/discovery/sinsa_01_main.jpg")))
                .isEqualTo("/images/discovery/sinsa_01_main.jpg");
    }

    @Test
    @DisplayName("자체 이미지가 없고 Google 장소면 사진 API 주소를 쓴다")
    void fallsBackToGooglePhotoPath() {
        assertThat(RestaurantImageUrl.of(restaurant(7L, "place-1", null))).isEqualTo("/api/v1/restaurants/7/photo");
    }

    @Test
    @DisplayName("자체 이미지도 Google 장소 ID도 없으면 null")
    void returnsNullWithoutAnySource() {
        assertThat(RestaurantImageUrl.of(restaurant(7L, null, null))).isNull();
    }
}
