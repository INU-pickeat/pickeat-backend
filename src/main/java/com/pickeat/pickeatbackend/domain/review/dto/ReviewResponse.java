package com.pickeat.pickeatbackend.domain.review.dto;

import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.review.entity.Review;
import com.pickeat.pickeatbackend.domain.review.entity.ReviewVisibility;
import java.time.Instant;
import java.util.List;

public record ReviewResponse(
        Long reviewId,
        Long pickId,
        Long restaurantId,
        String restaurantName,
        int rating,
        String content,
        FoodCategory foodCategory,
        CompanionType companionType,
        ReviewVisibility visibility,
        List<String> imageUrls,
        long likeCount,
        boolean likedByMe,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReviewResponse of(Review review, long likeCount, boolean likedByMe) {
        return new ReviewResponse(
                review.getId(),
                review.getPick().getId(),
                review.getRestaurant().getId(),
                review.getRestaurant().getName(),
                review.getRating(),
                review.getContent(),
                review.getFoodCategory(),
                review.getCompanionType(),
                review.getVisibility(),
                review.getImageUrls(),
                likeCount,
                likedByMe,
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
