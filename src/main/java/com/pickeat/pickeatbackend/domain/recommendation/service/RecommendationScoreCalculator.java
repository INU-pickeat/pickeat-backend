package com.pickeat.pickeatbackend.domain.recommendation.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

// 2026-09-22 기획서 갱신 M2 ADR: 평점을 3.0~5.0 구간으로 정규화(A2) + 거리 선형 정규화(B1)
// + 동행 적합 고정 가산점(C1).
// 2026-10-07: 평점은 리뷰 수로 베이지안 보정한 뒤 정규화한다(리뷰 1개짜리 5.0이 위로 올라오지 않게).
// 1km 반경(수도권·부산)은 걸어서 갈 만한 거리라 평점 0.8·거리 0.2, 5km 반경은 0.7·0.3.
@Component
public class RecommendationScoreCalculator {

    private static final double RATING_FLOOR = 3.0;
    private static final double RATING_RANGE = 2.0;
    // 리뷰가 PRIOR_REVIEW_COUNT개쯤 쌓여야 원래 평점에 가까워진다. 리뷰 수를 모르면 0개로 본다.
    private static final double PRIOR_RATING = 3.5;
    private static final double PRIOR_REVIEW_COUNT = 20.0;
    private static final double SHORT_RADIUS_METERS = 1000.0;
    private static final double SHORT_RADIUS_RATING_WEIGHT = 0.8;
    private static final double SHORT_RADIUS_DISTANCE_WEIGHT = 0.2;
    private static final double RATING_WEIGHT = 0.7;
    private static final double DISTANCE_WEIGHT = 0.3;
    private static final double COMPANION_MATCH_BONUS = 0.1;

    public ScoreBreakdown calculate(
            BigDecimal externalRating, Integer ratingCount, double distanceMeters, double radiusMeters,
            boolean companionMatch
    ) {
        double ratingScore = externalRating == null ? 0.0
                : Math.clamp((adjustedRating(externalRating, ratingCount) - RATING_FLOOR) / RATING_RANGE, 0.0, 1.0);
        double distanceScore = 1.0 - (distanceMeters / radiusMeters);
        boolean shortRadius = radiusMeters <= SHORT_RADIUS_METERS;

        double ratingContribution = (shortRadius ? SHORT_RADIUS_RATING_WEIGHT : RATING_WEIGHT) * ratingScore;
        double distanceContribution = (shortRadius ? SHORT_RADIUS_DISTANCE_WEIGHT : DISTANCE_WEIGHT) * distanceScore;
        double companionBonus = companionMatch ? COMPANION_MATCH_BONUS : 0.0;

        return new ScoreBreakdown(
                ratingContribution,
                distanceContribution,
                companionBonus,
                ratingContribution + distanceContribution + companionBonus
        );
    }

    private static double adjustedRating(BigDecimal externalRating, Integer ratingCount) {
        double count = ratingCount == null ? 0.0 : ratingCount;
        return (count * externalRating.doubleValue() + PRIOR_REVIEW_COUNT * PRIOR_RATING) / (count + PRIOR_REVIEW_COUNT);
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
