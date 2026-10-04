package com.pickeat.pickeatbackend.domain.discovery.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.discovery.entity.DiscoveryRegion;
import com.pickeat.pickeatbackend.domain.discovery.entity.DiscoverySpot;
import com.pickeat.pickeatbackend.domain.discovery.entity.DiscoverySpotRestaurant;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

class DiscoverySpotsResponseTest {

    @Test
    @DisplayName("사용자 후기가 없는 식당의 한줄평은 '후기가 없습니다.'로 내려간다")
    void fallsBackToNoReviewMessage() {
        DiscoverySpot spot = spotWith(item(1L, "운영자 소개 문구"));

        DiscoverySpotsResponse response = DiscoverySpotsResponse.from(List.of(spot), Map.of());

        assertThat(response.spots().get(0).restaurants().get(0).oneLineIntro()).isEqualTo("후기가 없습니다.");
    }

    @Test
    @DisplayName("사용자 후기 한줄평이 있으면 운영자 소개 문구 대신 그 값을 내려준다")
    void usesUserReviewOneLiner() {
        DiscoverySpot spot = spotWith(item(1L, "운영자 소개 문구"), item(2L, "운영자 소개 문구"));

        DiscoverySpotsResponse response =
                DiscoverySpotsResponse.from(List.of(spot), Map.of(1L, "양고기가 부드러워요", 2L, " "));

        List<DiscoverySpotsResponse.RestaurantItem> restaurants = response.spots().get(0).restaurants();
        assertThat(restaurants.get(0).oneLineIntro()).isEqualTo("양고기가 부드러워요");
        assertThat(restaurants.get(1).oneLineIntro()).isEqualTo(DiscoverySpotsResponse.NO_REVIEW_ONE_LINER);
    }

    private DiscoverySpot spotWith(DiscoverySpotRestaurant... items) {
        DiscoverySpot spot = BeanUtils.instantiateClass(DiscoverySpot.class);
        ReflectionTestUtils.setField(spot, "regionCode", DiscoveryRegion.values()[0]);
        ReflectionTestUtils.setField(spot, "regionName", "신사");
        ReflectionTestUtils.setField(spot, "displayOrder", 1);
        ReflectionTestUtils.setField(spot, "restaurants", List.of(items));
        return spot;
    }

    private DiscoverySpotRestaurant item(Long restaurantId, String curatedIntro) {
        Restaurant restaurant = Restaurant.builder().name("식당 " + restaurantId).latitude(37.5).longitude(127.0).build();
        ReflectionTestUtils.setField(restaurant, "id", restaurantId);
        DiscoverySpotRestaurant item = BeanUtils.instantiateClass(DiscoverySpotRestaurant.class);
        ReflectionTestUtils.setField(item, "restaurant", restaurant);
        ReflectionTestUtils.setField(item, "displayOrder", restaurantId.intValue());
        ReflectionTestUtils.setField(item, "oneLineIntro", curatedIntro);
        return item;
    }
}
