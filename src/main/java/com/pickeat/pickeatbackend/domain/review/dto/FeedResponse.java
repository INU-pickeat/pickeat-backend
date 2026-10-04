package com.pickeat.pickeatbackend.domain.review.dto;

import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.review.entity.Review;
import java.time.Instant;
import java.util.List;

// nextCursor가 null이면 마지막 페이지다. 다음 요청의 cursor로 그대로 넘긴다.
public record FeedResponse(List<Item> items, Long nextCursor) {

    public record Item(
            Long reviewId,
            Long restaurantId,
            String restaurantName,
            String authorNickname,
            String authorProfileImageUrl,
            int rating,
            String content,
            FoodCategory foodCategory,
            CompanionType companionType,
            List<String> imageUrls,
            long likeCount,
            boolean likedByMe,
            Instant createdAt
    ) {
        public static Item of(Review review, long likeCount, boolean likedByMe) {
            return new Item(
                    review.getId(),
                    review.getRestaurant().getId(),
                    review.getRestaurant().getName(),
                    review.getMember().getNickname(),
                    review.getMember().getProfileImageUrl(),
                    review.getRating(),
                    review.getContent(),
                    review.getFoodCategory(),
                    review.getCompanionType(),
                    review.getImageUrls(),
                    likeCount,
                    likedByMe,
                    review.getCreatedAt()
            );
        }
    }
}
