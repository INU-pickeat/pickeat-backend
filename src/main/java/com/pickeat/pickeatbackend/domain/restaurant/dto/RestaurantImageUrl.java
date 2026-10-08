package com.pickeat.pickeatbackend.domain.restaurant.dto;

import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;

// 응답에 내려줄 식당 대표 이미지 주소. 우리가 가진 이미지(탐색 스팟)가 있으면 그것을, 없으면 Google 사진으로
// 넘겨주는 API 주소를 돌려준다. Google에 사진이 실제로 있는지는 여기서 알 수 없어서, 사진이 없는 식당은
// 그 주소가 404(RESTAURANT_002)를 응답한다.
public final class RestaurantImageUrl {

    private RestaurantImageUrl() {
    }

    public static String of(Restaurant restaurant) {
        return of(restaurant.getId(), restaurant.getRepresentativeImageUrl(), restaurant.getGooglePlaceId());
    }

    // 엔티티 없이 집계 쿼리 결과만 있을 때 쓴다.
    public static String of(Long restaurantId, String ownImageUrl, String googlePlaceId) {
        if (ownImageUrl != null && !ownImageUrl.isBlank()) {
            return ownImageUrl;
        }
        if (restaurantId == null || googlePlaceId == null) {
            return null;
        }
        return photoPath(restaurantId);
    }

    public static String photoPath(Long restaurantId) {
        return "/api/v1/restaurants/" + restaurantId + "/photo";
    }
}
