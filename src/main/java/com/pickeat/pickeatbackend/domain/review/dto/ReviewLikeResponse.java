package com.pickeat.pickeatbackend.domain.review.dto;

public record ReviewLikeResponse(Long reviewId, boolean liked, long likeCount) {
}
