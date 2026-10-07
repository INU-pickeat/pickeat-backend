package com.pickeat.pickeatbackend.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecommendationScoreCalculatorTest {

    // 리뷰가 아주 많으면 베이지안 보정이 원래 평점과 거의 같아진다.
    private static final int MANY_REVIEWS = 10_000_000;
    private static final double TOLERANCE = 1e-5;

    private final RecommendationScoreCalculator calculator = new RecommendationScoreCalculator();

    @Test
    @DisplayName("최고 평점 최단 거리 동행 일치는 최고점이다")
    void returnsMaxScoreForBestRatingShortestDistanceAndCompanionMatch() {
        double score = calculator.calculate(BigDecimal.valueOf(5.0), MANY_REVIEWS, 0, 5000, true).totalScore();

        assertThat(score).isCloseTo(1.1, within(TOLERANCE));
    }

    @Test
    @DisplayName("최저 평점 최대 거리 동행 불일치는 최저점이다")
    void returnsMinScoreForWorstRatingMaxDistanceAndNoCompanionMatch() {
        double score = calculator.calculate(BigDecimal.ZERO, MANY_REVIEWS, 5000, 5000, false).totalScore();

        assertThat(score).isCloseTo(0.0, within(TOLERANCE));
    }

    @Test
    @DisplayName("평점이 없으면 평점 항은 0으로 계산된다")
    void treatsNullRatingAsZero() {
        double withNullRating = calculator.calculate(null, MANY_REVIEWS, 0, 5000, false).totalScore();
        double withZeroRating = calculator.calculate(BigDecimal.ZERO, MANY_REVIEWS, 0, 5000, false).totalScore();

        assertThat(withNullRating).isCloseTo(withZeroRating, within(TOLERANCE));
    }

    @Test
    @DisplayName("중간값 평점과 거리는 가중치대로 섞인다")
    void blendsMidRangeRatingAndDistanceByWeight() {
        double score = calculator.calculate(BigDecimal.valueOf(4.0), MANY_REVIEWS, 2500, 5000, false).totalScore();

        assertThat(score).isCloseTo(0.5, within(TOLERANCE));
    }

    @Test
    @DisplayName("평점이 3.0 미만이면 3.0과 동일하게 0점으로 취급된다")
    void clampsRatingBelowFloorToZero() {
        double belowFloor = calculator.calculate(BigDecimal.valueOf(1.0), MANY_REVIEWS, 1000, 5000, false).totalScore();
        double atFloor = calculator.calculate(BigDecimal.valueOf(3.0), MANY_REVIEWS, 1000, 5000, false).totalScore();

        assertThat(belowFloor).isCloseTo(atFloor, within(TOLERANCE));
    }

    @Test
    @DisplayName("동행 적합 가산점은 0.1점 차이를 만든다")
    void addsZeroPointOneForCompanionMatch() {
        double matched = calculator.calculate(BigDecimal.valueOf(3.0), MANY_REVIEWS, 1000, 5000, true).totalScore();
        double unmatched = calculator.calculate(BigDecimal.valueOf(3.0), MANY_REVIEWS, 1000, 5000, false).totalScore();

        assertThat(matched - unmatched).isCloseTo(0.1, within(TOLERANCE));
    }

    @Test
    @DisplayName("구성요소를 합치면 총점과 같다")
    void sumOfComponentsEqualsTotalScore() {
        RecommendationScoreCalculator.ScoreBreakdown breakdown =
                calculator.calculate(BigDecimal.valueOf(4.0), MANY_REVIEWS, 1500, 5000, true);

        assertThat(breakdown.ratingContribution() + breakdown.distanceContribution() + breakdown.companionBonus())
                .isCloseTo(breakdown.totalScore(), within(TOLERANCE));
    }

    @Test
    @DisplayName("리뷰 1개짜리 5.0은 리뷰 300개짜리 4.5보다 평점 점수가 낮다")
    void ranksFewReviewPerfectRatingBelowManyReviewHighRating() {
        double oneReview = calculator.calculate(BigDecimal.valueOf(5.0), 1, 0, 5000, false).ratingContribution();
        double manyReviews = calculator.calculate(BigDecimal.valueOf(4.5), 300, 0, 5000, false).ratingContribution();

        assertThat(oneReview).isLessThan(manyReviews);
    }

    @Test
    @DisplayName("리뷰 수는 기준 평점 3.5 쪽으로 20개 가중 평균해 보정한다")
    void shrinksRatingTowardPriorByReviewCount() {
        // (1 * 5.0 + 20 * 3.5) / 21 = 3.5714..., 정규화 (3.5714 - 3.0) / 2.0 = 0.2857, 가중치 0.7
        double contribution = calculator.calculate(BigDecimal.valueOf(5.0), 1, 0, 5000, false).ratingContribution();

        assertThat(contribution).isCloseTo(0.7 * (75.0 / 21.0 - 3.0) / 2.0, within(1e-9));
    }

    @Test
    @DisplayName("리뷰 수를 모르면 리뷰 0개로 보고 기준 평점을 쓴다")
    void treatsNullReviewCountAsZero() {
        double withNullCount = calculator.calculate(BigDecimal.valueOf(5.0), null, 0, 5000, false).ratingContribution();

        assertThat(withNullCount).isCloseTo(0.7 * (3.5 - 3.0) / 2.0, within(1e-9));
    }

    @Test
    @DisplayName("1km 반경은 평점 0.8, 거리 0.2 가중치를 쓴다")
    void usesRatingHeavyWeightsForOneKilometerRadius() {
        RecommendationScoreCalculator.ScoreBreakdown best =
                calculator.calculate(BigDecimal.valueOf(5.0), MANY_REVIEWS, 0, 1000, false);
        RecommendationScoreCalculator.ScoreBreakdown halfway =
                calculator.calculate(BigDecimal.valueOf(5.0), MANY_REVIEWS, 500, 1000, false);

        assertThat(best.ratingContribution()).isCloseTo(0.8, within(TOLERANCE));
        assertThat(best.distanceContribution()).isCloseTo(0.2, within(1e-9));
        assertThat(halfway.distanceContribution()).isCloseTo(0.1, within(1e-9));
    }
}
