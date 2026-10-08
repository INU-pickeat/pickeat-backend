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
        BigDecimal priceRangeStart,
        BigDecimal priceRangeEnd,
        String priceCurrencyCode,
        String representativeImageUrl,
        Boolean suitableForDate,
        Boolean suitableForFamily,
        Boolean suitableForChildren,
        Boolean suitableForSolo,
        Boolean suitableForGroup,
        Boolean suitableForDogs,
        String oneLineReview
) {

    // oneLineReview: 앱 내 사용자 후기에서 뽑은 한줄평. 후기가 없으면 호출한 쪽에서 ReviewOneLiner.NO_REVIEW를 넘긴다.
    public static RestaurantResponse from(Restaurant restaurant, String oneLineReview) {
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
                restaurant.getPriceRangeStart(),
                restaurant.getPriceRangeEnd(),
                restaurant.getPriceCurrencyCode(),
                restaurant.getRepresentativeImageUrl(),
                restaurant.getSuitableForDate(),
                restaurant.getSuitableForFamily(),
                restaurant.getSuitableForChildren(),
                restaurant.getSuitableForSolo(),
                restaurant.getSuitableForGroup(),
                restaurant.getSuitableForDogs(),
                oneLineReview
        );
    }
}
