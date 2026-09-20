package com.pickeat.pickeatbackend.domain.restaurant.service;

import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlaceResponse;
import com.pickeat.pickeatbackend.domain.restaurant.dto.RestaurantResponse;
import com.pickeat.pickeatbackend.domain.restaurant.entity.DataProvider;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.domain.restaurant.exception.RestaurantErrorCode;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurant(Long restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .map(RestaurantResponse::from)
                .orElseThrow(() -> new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND));
    }

    @Transactional
    public Restaurant upsertFromGoogle(GooglePlaceResponse place) {
        Restaurant restaurant = restaurantRepository.findByGooglePlaceId(place.id())
                .orElseGet(() -> Restaurant.builder()
                        .dataProvider(DataProvider.GOOGLE)
                        .googlePlaceId(place.id())
                        .name(place.displayName().text())
                        .latitude(place.location().latitude())
                        .longitude(place.location().longitude())
                        .build());

        // Google이 이번 응답에서 유형을 못 주거나 매핑이 안 되면 기존 분류를 유지한다.
        FoodCategory foodCategory = FoodCategory.fromGooglePrimaryType(place.primaryType())
                .orElse(restaurant.getFoodCategory());
        BigDecimal rating = place.rating() == null ? null : BigDecimal.valueOf(place.rating());

        restaurant.updateFromGoogle(
                place.displayName().text(),
                foodCategory,
                place.formattedAddress(),
                place.location().latitude(),
                place.location().longitude(),
                rating,
                place.userRatingCount()
        );

        return restaurantRepository.save(restaurant);
    }
}
