package com.pickeat.pickeatbackend.domain.review.dto;

import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.review.entity.Review;
import com.pickeat.pickeatbackend.domain.review.entity.ReviewVisibility;
import java.time.Instant;
import java.util.List;

public record MyReviewsResponse(List<Item> reviews) {

    public static MyReviewsResponse from(List<Review> reviews) {
        return new MyReviewsResponse(reviews.stream().map(Item::from).toList());
    }

    public record Item(
            Long reviewId,
            Long pickId,
            Long restaurantId,
            String restaurantName,
            String content,
            FoodCategory foodCategory,
            CompanionType companionType,
            ReviewVisibility visibility,
            List<String> imageUrls,
            Instant selectedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        private static Item from(Review review) {
            return new Item(
                    review.getId(),
                    review.getPick().getId(),
                    review.getRestaurant().getId(),
                    review.getRestaurant().getName(),
                    review.getContent(),
                    review.getFoodCategory(),
                    review.getCompanionType(),
                    review.getVisibility(),
                    review.getImageUrls(),
                    review.getPick().getSelectedAt(),
                    review.getCreatedAt(),
                    review.getUpdatedAt());
        }
    }
}
