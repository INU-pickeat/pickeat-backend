package com.pickeat.pickeatbackend.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RecommendationScoreCalculatorTest {

    private final RecommendationScoreCalculator calculator = new RecommendationScoreCalculator();

    @Test
    void 최고_평점_최단_거리_동행_일치는_최고점이다() {
        double score = calculator.calculate(BigDecimal.valueOf(5.0), 0, true);

        assertThat(score).isCloseTo(1.1, within(1e-9));
    }

    @Test
    void 최저_평점_최대_거리_동행_불일치는_최저점이다() {
        double score = calculator.calculate(BigDecimal.ZERO, 5000, false);

        assertThat(score).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void 평점이_없으면_평점_항은_0으로_계산된다() {
        double withNullRating = calculator.calculate(null, 0, false);
        double withZeroRating = calculator.calculate(BigDecimal.ZERO, 0, false);

        assertThat(withNullRating).isCloseTo(withZeroRating, within(1e-9));
    }

    @Test
    void 중간값_평점과_거리는_가중치대로_섞인다() {
        double score = calculator.calculate(BigDecimal.valueOf(2.5), 2500, false);

        assertThat(score).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void 동행_적합_가산점은_0_1점_차이를_만든다() {
        double matched = calculator.calculate(BigDecimal.valueOf(3.0), 1000, true);
        double unmatched = calculator.calculate(BigDecimal.valueOf(3.0), 1000, false);

        assertThat(matched - unmatched).isCloseTo(0.1, within(1e-9));
    }
}
