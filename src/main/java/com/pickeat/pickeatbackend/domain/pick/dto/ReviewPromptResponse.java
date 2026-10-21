package com.pickeat.pickeatbackend.domain.pick.dto;

import com.pickeat.pickeatbackend.domain.pick.entity.Pick;
import com.pickeat.pickeatbackend.domain.restaurant.dto.RestaurantImageUrl;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import java.time.Instant;

// 홈 상단 후기 작성 유도 배너. 띄울 Pick이 없으면 pick이 null이다.
public record ReviewPromptResponse(Item pick) {

    public static ReviewPromptResponse empty() {
        return new ReviewPromptResponse(null);
    }

    public static ReviewPromptResponse from(Pick pick) {
        return new ReviewPromptResponse(new Item(
                pick.getId(),
                pick.getRestaurant().getId(),
                pick.getRestaurant().getName(),
                pick.getRestaurant().getFoodCategory(),
                RestaurantImageUrl.of(pick.getRestaurant()),
                pick.getSelectedAt()));
    }

    public record Item(
            Long pickId,
            Long restaurantId,
            String restaurantName,
            FoodCategory foodCategory,
            // 자체 이미지 → Google 사진 API 주소 → null 순. 최근 Pick 목록의 representativeImageUrl과 같은 규칙이다.
            String representativeImageUrl,
            Instant selectedAt
    ) {
    }
}
