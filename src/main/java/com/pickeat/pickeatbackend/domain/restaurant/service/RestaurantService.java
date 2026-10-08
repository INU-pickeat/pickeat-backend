package com.pickeat.pickeatbackend.domain.restaurant.service;

import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlaceResponse;
import com.pickeat.pickeatbackend.domain.restaurant.dto.RestaurantNavigationLinksResponse;
import com.pickeat.pickeatbackend.domain.restaurant.dto.RestaurantResponse;
import com.pickeat.pickeatbackend.domain.restaurant.entity.DataProvider;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.domain.restaurant.exception.RestaurantErrorCode;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import com.pickeat.pickeatbackend.domain.review.dto.ReviewOneLiner;
import com.pickeat.pickeatbackend.domain.review.service.ReviewSummaryService;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final ReviewSummaryService reviewSummaryService;

    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND));
        String oneLineReview = reviewSummaryService.getOneLineReviews(List.of(restaurantId))
                .getOrDefault(restaurantId, ReviewOneLiner.NO_REVIEW);
        return RestaurantResponse.from(restaurant, oneLineReview);
    }

    @Transactional(readOnly = true)
    public RestaurantNavigationLinksResponse getNavigationLinks(Long restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .map(RestaurantNavigationLinksResponse::from)
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

        // 이름에 보정 키워드가 있으면 Google 유형보다 그 카테고리를 우선한다(예: 칼국수·장어 → 한식, 양꼬치 → 중식).
        // 없으면 Google 유형을 따르고, Google이 유형을 못 주거나 매핑이 안 되면 기존 분류를 유지한다.
        Optional<FoodCategoryNameOverride.Categories> nameOverride =
                FoodCategoryNameOverride.find(place.displayName().text());
        FoodCategory foodCategory = nameOverride.map(FoodCategoryNameOverride.Categories::primary)
                .or(() -> FoodCategory.fromGooglePrimaryType(place.primaryType()))
                .orElse(restaurant.getFoodCategory());
        BigDecimal rating = place.rating() == null ? null : BigDecimal.valueOf(place.rating());
        GooglePlaceResponse.PriceRange priceRange = place.priceRange();
        BigDecimal priceStart = priceRange == null || priceRange.startPrice() == null
                ? null : priceRange.startPrice().amount();
        BigDecimal priceEnd = priceRange == null || priceRange.endPrice() == null
                ? null : priceRange.endPrice().amount();
        String currencyCode = priceRange == null ? null : firstCurrencyCode(priceRange);

        Boolean suitableForFamily = and(place.goodForChildren(), place.goodForGroups());
        Boolean suitableForChildren = or(place.goodForChildren(), place.menuForChildren());

        restaurant.updateFromGoogle(
                place.displayName().text(),
                foodCategory,
                place.formattedAddress(),
                place.location().latitude(),
                place.location().longitude(),
                rating,
                place.userRatingCount(),
                priceStart,
                priceEnd,
                currencyCode,
                suitableForFamily,
                suitableForChildren,
                place.goodForGroups(),
                place.allowsDogs()
        );
        restaurant.updateSecondaryFoodCategory(
                nameOverride.map(FoodCategoryNameOverride.Categories::secondary).orElse(null));
        restaurant.updateOpeningHours(OpeningHours.toWeekMinutes(place.regularOpeningHours()));

        return restaurantRepository.save(restaurant);
    }

    private String firstCurrencyCode(GooglePlaceResponse.PriceRange priceRange) {
        if (priceRange.startPrice() != null) {
            return priceRange.startPrice().currencyCode();
        }
        return priceRange.endPrice() == null ? null : priceRange.endPrice().currencyCode();
    }

    private Boolean and(Boolean left, Boolean right) {
        if (Boolean.FALSE.equals(left) || Boolean.FALSE.equals(right)) {
            return false;
        }
        return Boolean.TRUE.equals(left) && Boolean.TRUE.equals(right) ? true : null;
    }

    private Boolean or(Boolean left, Boolean right) {
        if (Boolean.TRUE.equals(left) || Boolean.TRUE.equals(right)) {
            return true;
        }
        return Boolean.FALSE.equals(left) && Boolean.FALSE.equals(right) ? false : null;
    }
}
