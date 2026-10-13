package com.pickeat.pickeatbackend.domain.review.repository;

import java.time.Instant;

public record LikeNotification(
        Long reviewId, String restaurantName, String actorNickname, String actorProfileImageUrl, Instant createdAt) {
}
