package com.pickeat.pickeatbackend.domain.discovery.dto;

import com.pickeat.pickeatbackend.domain.discovery.entity.DiscoverySpot;
import com.pickeat.pickeatbackend.domain.discovery.entity.DiscoverySpotRestaurant;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import java.util.List;

public record DiscoverySpotsResponse(List<Spot> spots) {

    public static DiscoverySpotsResponse from(List<DiscoverySpot> discoverySpots) {
        return new DiscoverySpotsResponse(discoverySpots.stream().map(Spot::from).toList());
    }

    public record Spot(
            String regionCode,
            String regionName,
            Integer displayOrder,
            List<RestaurantItem> restaurants
    ) {
        private static Spot from(DiscoverySpot spot) {
            return new Spot(
                    spot.getRegionCode().name(),
                    spot.getRegionName(),
                    spot.getDisplayOrder(),
                    spot.getRestaurants().stream().map(RestaurantItem::from).toList()
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
        private static RestaurantItem from(DiscoverySpotRestaurant item) {
            Restaurant restaurant = item.getRestaurant();
            return new RestaurantItem(
                    restaurant.getId(),
                    restaurant.getName(),
                    restaurant.getFoodCategory(),
                    item.getOneLineIntro(),
                    restaurant.getAddress(),
                    restaurant.getOpeningHoursText(),
                    restaurant.getPhoneNumber(),
                    restaurant.getRepresentativeImageUrl(),
                    item.getDisplayOrder()
            );
        }
    }
}
