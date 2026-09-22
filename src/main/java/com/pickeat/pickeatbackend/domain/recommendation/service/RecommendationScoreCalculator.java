package com.pickeat.pickeatbackend.domain.recommendation.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

// 2026-09-22 기획서 갱신 M2 ADR: 평점을 3.0~5.0 구간으로 정규화(A2) + 거리 선형 정규화(B1)
// + 동행 적합 고정 가산점(C1). 실사용 데이터가 쌓이기 전까지는 이 상수를 튜닝하지 않는다.
@Component
public class RecommendationScoreCalculator {

    private static final double RATING_FLOOR = 3.0;
    private static final double RATING_RANGE = 2.0;
    private static final double SEARCH_RADIUS_METERS = 5000.0;
    private static final double RATING_WEIGHT = 0.7;
    private static final double DISTANCE_WEIGHT = 0.3;
    private static final double COMPANION_MATCH_BONUS = 0.1;

    public ScoreBreakdown calculate(BigDecimal externalRating, double distanceMeters, boolean companionMatch) {
        double ratingScore = externalRating == null ? 0.0
                : Math.clamp((externalRating.doubleValue() - RATING_FLOOR) / RATING_RANGE, 0.0, 1.0);
        double distanceScore = 1.0 - (distanceMeters / SEARCH_RADIUS_METERS);

        double ratingContribution = RATING_WEIGHT * ratingScore;
        double distanceContribution = DISTANCE_WEIGHT * distanceScore;
        double companionBonus = companionMatch ? COMPANION_MATCH_BONUS : 0.0;

        return new ScoreBreakdown(
                ratingContribution,
                distanceContribution,
                companionBonus,
                ratingContribution + distanceContribution + companionBonus
        );
    }

    // recommendation_candidates 컬럼과 1:1 대응 — 세션 조회 시 재계산 없이 그대로 복원한다.
    public record ScoreBreakdown(
            double ratingContribution,
            double distanceContribution,
            double companionBonus,
            double totalScore
    ) {
    }
}
