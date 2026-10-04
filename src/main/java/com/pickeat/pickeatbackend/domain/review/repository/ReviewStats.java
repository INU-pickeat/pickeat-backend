package com.pickeat.pickeatbackend.domain.review.repository;

// averageRating은 후기가 하나도 없으면 null이다.
public record ReviewStats(Long reviewCount, Double averageRating) {
}
