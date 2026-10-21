package com.pickeat.pickeatbackend.domain.discovery.dto;

import com.pickeat.pickeatbackend.domain.discovery.entity.DiscoverySpot;
import com.pickeat.pickeatbackend.domain.discovery.entity.DiscoverySpotRestaurant;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewOneLiner;
import java.util.List;
import java.util.Map;

public record DiscoverySpotsResponse(List<Spot> spots) {

    // 한줄평 우선순위(2026-10-21): 최신 공개 사용자 후기 → 운영자 한줄평(discovery_spot_restaurants.one_line_intro)
    // → 이 문구. 운영자 한줄평은 NOT NULL이라 실제로는 마지막 단계까지 거의 가지 않는다.
    public static final String NO_REVIEW_ONE_LINER = ReviewOneLiner.NO_REVIEW;

    // oneLineReviews: restaurantId → 사용자 후기에서 뽑은 한줄평.
    public static DiscoverySpotsResponse from(List<DiscoverySpot> discoverySpots, Map<Long, String> oneLineReviews) {
        return new DiscoverySpotsResponse(
                discoverySpots.stream().map(spot -> Spot.from(spot, oneLineReviews)).toList());
    }

    public record Spot(
            String regionCode,
            String regionName,
            Integer displayOrder,
            List<RestaurantItem> restaurants
    ) {
        private static Spot from(DiscoverySpot spot, Map<Long, String> oneLineReviews) {
            return new Spot(
                    spot.getRegionCode().name(),
                    spot.getRegionName(),
                    spot.getDisplayOrder(),
                    spot.getRestaurants().stream()
                            .map(item -> RestaurantItem.from(item, oneLineReviews))
                            .toList()
            );
        }
    }

    public record RestaurantItem(
            Long restaurantId,
            String name,
            FoodCategory foodCategory,
            String oneLineIntro,
            String address,
            String openingHoursText,
            String phoneNumber,
            String representativeImageUrl,
            Integer displayOrder
    ) {
        private static RestaurantItem from(DiscoverySpotRestaurant item, Map<Long, String> oneLineReviews) {
            Restaurant restaurant = item.getRestaurant();
            return new RestaurantItem(
                    restaurant.getId(),
                    restaurant.getName(),
                    restaurant.getFoodCategory(),
                    oneLineIntro(oneLineReviews.get(restaurant.getId()), item.getOneLineIntro()),
                    restaurant.getAddress(),
                    restaurant.getOpeningHoursText(),
                    restaurant.getPhoneNumber(),
                    restaurant.getRepresentativeImageUrl(),
                    item.getDisplayOrder()
            );
        }

        private static String oneLineIntro(String userReview, String curatedIntro) {
            if (userReview != null && !userReview.isBlank()) {
                return userReview;
            }
            if (curatedIntro != null && !curatedIntro.isBlank()) {
                return curatedIntro;
            }
            return NO_REVIEW_ONE_LINER;
        }
    }
}
