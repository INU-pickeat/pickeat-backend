package com.pickeat.pickeatbackend.domain.recommendation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.recommendation.dto.RecommendationRequest.PriceRange;
import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RecommendationRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    @DisplayName("유효한 요청은 위반이 없다")
    void hasNoViolationsWhenValid() {
        RecommendationRequest request = new RecommendationRequest(
                Set.of(FoodCategory.KOREAN), CompanionType.DATE, null, 37.5, 127.0);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("가격대가 유효하면 위반이 없다")
    void hasNoViolationsWhenPriceRangeValid() {
        RecommendationRequest request = new RecommendationRequest(
                Set.of(FoodCategory.KOREAN), CompanionType.DATE,
                new PriceRange(BigDecimal.valueOf(10000), BigDecimal.valueOf(30000)), 37.5, 127.0);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("가격 하한이 음수면 위반된다")
    void violatesWhenPriceRangeMinNegative() {
        RecommendationRequest request = new RecommendationRequest(
                Set.of(FoodCategory.KOREAN), CompanionType.DATE,
                new PriceRange(BigDecimal.valueOf(-1), BigDecimal.valueOf(30000)), 37.5, 127.0);

        assertThat(propertyPaths(request)).containsExactly("priceRange.min");
    }

    @Test
    @DisplayName("가격 하한이 상한보다 크면 위반된다")
    void violatesWhenPriceRangeMinGreaterThanMax() {
        RecommendationRequest request = new RecommendationRequest(
                Set.of(FoodCategory.KOREAN), CompanionType.DATE,
                new PriceRange(BigDecimal.valueOf(30000), BigDecimal.valueOf(10000)), 37.5, 127.0);

        assertThat(propertyPaths(request)).containsExactly("priceRange.validRange");
    }

    @Test
    @DisplayName("음식 카테고리가 비어있으면 위반된다")
    void violatesWhenFoodCategoriesEmpty() {
        RecommendationRequest request = new RecommendationRequest(
                Set.of(), CompanionType.DATE, null, 37.5, 127.0);

        assertThat(propertyPaths(request)).containsExactly("foodCategories");
    }

    @Test
    @DisplayName("동행 유형이 없으면 위반된다")
    void violatesWhenCompanionTypeMissing() {
        RecommendationRequest request = new RecommendationRequest(
                Set.of(FoodCategory.KOREAN), null, null, 37.5, 127.0);

        assertThat(propertyPaths(request)).containsExactly("companionType");
    }

    @Test
    @DisplayName("위도가 없으면 위반된다")
    void violatesWhenLatitudeMissing() {
        RecommendationRequest request = new RecommendationRequest(
                Set.of(FoodCategory.KOREAN), CompanionType.DATE, null, null, 127.0);

        assertThat(propertyPaths(request)).containsExactly("latitude");
    }

    @Test
    @DisplayName("경도가 없으면 위반된다")
    void violatesWhenLongitudeMissing() {
        RecommendationRequest request = new RecommendationRequest(
                Set.of(FoodCategory.KOREAN), CompanionType.DATE, null, 37.5, null);

        assertThat(propertyPaths(request)).containsExactly("longitude");
    }

    @ParameterizedTest
    @DisplayName("위도가 범위를 벗어나면 위반된다")
    @CsvSource({"-91.0", "91.0"})
    void violatesWhenLatitudeOutOfRange(double latitude) {
        RecommendationRequest request = new RecommendationRequest(
                Set.of(FoodCategory.KOREAN), CompanionType.DATE, null, latitude, 127.0);

        assertThat(propertyPaths(request)).containsExactly("latitude");
    }

    @ParameterizedTest
    @DisplayName("경도가 범위를 벗어나면 위반된다")
    @CsvSource({"-181.0", "181.0"})
    void violatesWhenLongitudeOutOfRange(double longitude) {
        RecommendationRequest request = new RecommendationRequest(
                Set.of(FoodCategory.KOREAN), CompanionType.DATE, null, 37.5, longitude);

        assertThat(propertyPaths(request)).containsExactly("longitude");
    }

    private static Set<String> propertyPaths(RecommendationRequest request) {
        Set<ConstraintViolation<RecommendationRequest>> violations = validator.validate(request);
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
