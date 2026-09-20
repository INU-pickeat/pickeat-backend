package com.pickeat.pickeatbackend.domain.restaurant.dto;

import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import java.math.BigDecimal;

public record RestaurantResponse(
        Long id,
        String name,
        FoodCategory foodCategory,
        String address,
        Double latitude,
        Double longitude,
        String phoneNumber,
        String openingHoursText,
        BigDecimal externalRating,
        Integer externalRatingCount,
        String priceLevel,
        String representativeImageUrl,
        Boolean suitableForDate,
        Boolean suitableForFriends,
        Boolean suitableForFamily,
        Boolean suitableForSolo,
        Boolean suitableForGroupDinner
) {

    public static RestaurantResponse from(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getFoodCategory(),
                restaurant.getAddress(),
                restaurant.getLatitude(),
                restaurant.getLongitude(),
                restaurant.getPhoneNumber(),
                restaurant.getOpeningHoursText(),
                restaurant.getExternalRating(),
                restaurant.getExternalRatingCount(),
                restaurant.getPriceLevel(),
                restaurant.getRepresentativeImageUrl(),
                restaurant.getSuitableForDate(),
                restaurant.getSuitableForFriends(),
                restaurant.getSuitableForFamily(),
                restaurant.getSuitableForSolo(),
                restaurant.getSuitableForGroupDinner()
        );
    }
}
