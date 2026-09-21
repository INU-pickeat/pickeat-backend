package com.pickeat.pickeatbackend.domain.recommendation.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

// ADR-M2-1: 원시 평점(A1) + 거리 선형 정규화(B1) + 동행 적합 고정 가산점(C1).
// 실사용 데이터가 쌓이기 전까지는 이 상수를 튜닝하지 않는다.
@Component
public class RecommendationScoreCalculator {

    private static final double MAX_RATING = 5.0;
    private static final double SEARCH_RADIUS_METERS = 5000.0;
    private static final double RATING_WEIGHT = 0.6;
    private static final double DISTANCE_WEIGHT = 0.4;
    private static final double COMPANION_MATCH_BONUS = 0.1;

    public double calculate(BigDecimal externalRating, double distanceMeters, boolean companionMatch) {
        double ratingScore = externalRating == null ? 0.0 : externalRating.doubleValue() / MAX_RATING;
        double distanceScore = 1.0 - (distanceMeters / SEARCH_RADIUS_METERS);
        double companionBonus = companionMatch ? COMPANION_MATCH_BONUS : 0.0;

        return RATING_WEIGHT * ratingScore + DISTANCE_WEIGHT * distanceScore + companionBonus;
    }
}
