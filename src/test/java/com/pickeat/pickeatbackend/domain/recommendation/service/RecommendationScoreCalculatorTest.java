package com.pickeat.pickeatbackend.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RecommendationScoreCalculatorTest {

    private final RecommendationScoreCalculator calculator = new RecommendationScoreCalculator();

    @Test
    void 최고_평점_최단_거리_동행_일치는_최고점이다() {
        double score = calculator.calculate(BigDecimal.valueOf(5.0), 0, true).totalScore();

        assertThat(score).isCloseTo(1.1, within(1e-9));
    }

    @Test
    void 최저_평점_최대_거리_동행_불일치는_최저점이다() {
        double score = calculator.calculate(BigDecimal.ZERO, 5000, false).totalScore();

        assertThat(score).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void 평점이_없으면_평점_항은_0으로_계산된다() {
        double withNullRating = calculator.calculate(null, 0, false).totalScore();
        double withZeroRating = calculator.calculate(BigDecimal.ZERO, 0, false).totalScore();

        assertThat(withNullRating).isCloseTo(withZeroRating, within(1e-9));
    }

    @Test
    void 중간값_평점과_거리는_가중치대로_섞인다() {
        double score = calculator.calculate(BigDecimal.valueOf(2.5), 2500, false).totalScore();

        assertThat(score).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void 동행_적합_가산점은_0_1점_차이를_만든다() {
        double matched = calculator.calculate(BigDecimal.valueOf(3.0), 1000, true).totalScore();
        double unmatched = calculator.calculate(BigDecimal.valueOf(3.0), 1000, false).totalScore();

        assertThat(matched - unmatched).isCloseTo(0.1, within(1e-9));
    }

    @Test
    void 구성요소를_합치면_총점과_같다() {
        RecommendationScoreCalculator.ScoreBreakdown breakdown =
                calculator.calculate(BigDecimal.valueOf(4.0), 1500, true);

        assertThat(breakdown.ratingContribution() + breakdown.distanceContribution() + breakdown.companionBonus())
                .isCloseTo(breakdown.totalScore(), within(1e-9));
    }
}
