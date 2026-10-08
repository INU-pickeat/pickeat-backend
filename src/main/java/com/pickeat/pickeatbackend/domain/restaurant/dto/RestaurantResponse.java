package com.pickeat.pickeatbackend.domain.restaurant.dto;

import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.domain.restaurant.service.RestaurantPhotoService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
        String oneLineReview,
        List<String> imageUrls
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
                RestaurantImageUrl.of(restaurant),
                restaurant.getSuitableForDate(),
                restaurant.getSuitableForFamily(),
                restaurant.getSuitableForChildren(),
                restaurant.getSuitableForSolo(),
                restaurant.getSuitableForGroup(),
                restaurant.getSuitableForDogs(),
                oneLineReview,
                imageUrls(restaurant)
        );
    }

    // 상세 화면용 이미지 목록(최대 3장). 첫 번째는 representativeImageUrl과 같다. 자체 이미지가 있으면 그것을
    // 먼저 두고 나머지를 Google 사진으로 채운다. Google에 사진이 그만큼 없으면 해당 주소는 404를 응답한다.
    private static List<String> imageUrls(Restaurant restaurant) {
        List<String> urls = new ArrayList<>();
        String own = restaurant.getRepresentativeImageUrl();
        if (own != null && !own.isBlank()) {
            urls.add(own);
        }
        if (restaurant.getId() != null && restaurant.getGooglePlaceId() != null) {
            String photoPath = RestaurantImageUrl.photoPath(restaurant.getId());
            for (int index = 0; urls.size() < RestaurantPhotoService.MAX_PHOTOS; index++) {
                urls.add(index == 0 ? photoPath : photoPath + "?index=" + index);
            }
        }
        return List.copyOf(urls);
    }
}
