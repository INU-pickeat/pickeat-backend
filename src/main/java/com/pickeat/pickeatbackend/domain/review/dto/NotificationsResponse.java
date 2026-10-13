package com.pickeat.pickeatbackend.domain.review.dto;

import com.pickeat.pickeatbackend.domain.review.repository.LikeNotification;
import java.time.Instant;
import java.util.List;

public record NotificationsResponse(List<Item> notifications) {

    public enum Type { REVIEW_LIKE }

    // message는 "{닉네임}님이 회원님의 기록을 좋아해요" 형태라 프론트가 그대로 보여 주면 된다.
    public record Item(
            Type type,
            Long reviewId,
            String restaurantName,
            String actorNickname,
            String actorProfileImageUrl,
            String message,
            Instant createdAt
    ) {
        static Item from(LikeNotification like) {
            return new Item(
                    Type.REVIEW_LIKE,
                    like.reviewId(),
                    like.restaurantName(),
                    like.actorNickname(),
                    like.actorProfileImageUrl(),
                    like.actorNickname() + "님이 회원님의 기록을 좋아해요",
                    like.createdAt()
            );
        }
    }

    public static NotificationsResponse from(List<LikeNotification> likes) {
        return new NotificationsResponse(likes.stream().map(Item::from).toList());
    }
}
