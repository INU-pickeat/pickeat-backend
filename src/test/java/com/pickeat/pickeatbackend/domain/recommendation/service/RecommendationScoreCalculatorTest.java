package com.pickeat.pickeatbackend.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecommendationScoreCalculatorTest {

    private final RecommendationScoreCalculator calculator = new RecommendationScoreCalculator();

    @Test
    @DisplayName("최고 평점 최단 거리 동행 일치는 최고점이다")
    void returnsMaxScoreForBestRatingShortestDistanceAndCompanionMatch() {
        double score = calculator.calculate(BigDecimal.valueOf(5.0), 0, true).totalScore();

        assertThat(score).isCloseTo(1.1, within(1e-9));
    }

    @Test
    @DisplayName("최저 평점 최대 거리 동행 불일치는 최저점이다")
    void returnsMinScoreForWorstRatingMaxDistanceAndNoCompanionMatch() {
        double score = calculator.calculate(BigDecimal.ZERO, 5000, false).totalScore();

        assertThat(score).isCloseTo(0.0, within(1e-9));
    }

    @Test
    @DisplayName("평점이 없으면 평점 항은 0으로 계산된다")
    void treatsNullRatingAsZero() {
        double withNullRating = calculator.calculate(null, 0, false).totalScore();
        double withZeroRating = calculator.calculate(BigDecimal.ZERO, 0, false).totalScore();

        assertThat(withNullRating).isCloseTo(withZeroRating, within(1e-9));
    }

    @Test
    @DisplayName("중간값 평점과 거리는 가중치대로 섞인다")
    void blendsMidRangeRatingAndDistanceByWeight() {
        double score = calculator.calculate(BigDecimal.valueOf(4.0), 2500, false).totalScore();

        assertThat(score).isCloseTo(0.5, within(1e-9));
    }

    @Test
    @DisplayName("평점이 3.0 미만이면 3.0과 동일하게 0점으로 취급된다")
    void clampsRatingBelowFloorToZero() {
        double belowFloor = calculator.calculate(BigDecimal.valueOf(1.0), 1000, false).totalScore();
        double atFloor = calculator.calculate(BigDecimal.valueOf(3.0), 1000, false).totalScore();

        assertThat(belowFloor).isCloseTo(atFloor, within(1e-9));
    }

    @Test
    @DisplayName("동행 적합 가산점은 0.1점 차이를 만든다")
    void addsZeroPointOneForCompanionMatch() {
        double matched = calculator.calculate(BigDecimal.valueOf(3.0), 1000, true).totalScore();
        double unmatched = calculator.calculate(BigDecimal.valueOf(3.0), 1000, false).totalScore();

        assertThat(matched - unmatched).isCloseTo(0.1, within(1e-9));
    }

    @Test
    @DisplayName("구성요소를 합치면 총점과 같다")
    void sumOfComponentsEqualsTotalScore() {
        RecommendationScoreCalculator.ScoreBreakdown breakdown =
                calculator.calculate(BigDecimal.valueOf(4.0), 1500, true);

        assertThat(breakdown.ratingContribution() + breakdown.distanceContribution() + breakdown.companionBonus())
                .isCloseTo(breakdown.totalScore(), within(1e-9));
    }
}
