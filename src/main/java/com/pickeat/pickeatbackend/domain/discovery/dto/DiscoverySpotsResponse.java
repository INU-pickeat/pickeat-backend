package com.pickeat.pickeatbackend.domain.discovery.dto;

import com.pickeat.pickeatbackend.domain.discovery.entity.DiscoverySpot;
import com.pickeat.pickeatbackend.domain.discovery.entity.DiscoverySpotRestaurant;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewOneLiner;
import java.util.List;
import java.util.Map;

public record DiscoverySpotsResponse(List<Spot> spots) {

    // 한줄평은 앱 내 사용자 후기로만 채운다. 후기가 없는 식당은 이 문구를 그대로 내려준다.
    public static final String NO_REVIEW_ONE_LINER = ReviewOneLiner.NO_REVIEW;

    // oneLineReviews: restaurantId → 사용자 후기에서 뽑은 한줄평. 없는 식당은 NO_REVIEW_ONE_LINER로 채운다.
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
            String oneLineReview = oneLineReviews.get(restaurant.getId());
            return new RestaurantItem(
                    restaurant.getId(),
                    restaurant.getName(),
                    restaurant.getFoodCategory(),
                    oneLineReview == null || oneLineReview.isBlank() ? NO_REVIEW_ONE_LINER : oneLineReview,
                    restaurant.getAddress(),
                    restaurant.getOpeningHoursText(),
                    restaurant.getPhoneNumber(),
                    restaurant.getRepresentativeImageUrl(),
                    item.getDisplayOrder()
            );
        }
    }
}
