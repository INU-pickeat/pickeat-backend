package com.pickeat.pickeatbackend.domain.recommendation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
    void 유효한_요청은_위반이_없다() {
        RecommendationRequest request = new RecommendationRequest(
                Set.of(FoodCategory.KOREAN), CompanionType.DATE, 37.5, 127.0);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void 음식_카테고리가_비어있으면_위반된다() {
        RecommendationRequest request = new RecommendationRequest(
                Set.of(), CompanionType.DATE, 37.5, 127.0);

        assertThat(propertyPaths(request)).containsExactly("foodCategories");
    }

    @Test
    void 동행_유형이_없으면_위반된다() {
        RecommendationRequest request = new RecommendationRequest(
                Set.of(FoodCategory.KOREAN), null, 37.5, 127.0);

        assertThat(propertyPaths(request)).containsExactly("companionType");
    }

    @Test
    void 위도가_없으면_위반된다() {
        RecommendationRequest request = new RecommendationRequest(
                Set.of(FoodCategory.KOREAN), CompanionType.DATE, null, 127.0);

        assertThat(propertyPaths(request)).containsExactly("latitude");
    }

    @Test
    void 경도가_없으면_위반된다() {
        RecommendationRequest request = new RecommendationRequest(
                Set.of(FoodCategory.KOREAN), CompanionType.DATE, 37.5, null);

        assertThat(propertyPaths(request)).containsExactly("longitude");
    }

    @ParameterizedTest
    @CsvSource({"-91.0", "91.0"})
    void 위도가_범위를_벗어나면_위반된다(double latitude) {
        RecommendationRequest request = new RecommendationRequest(
                Set.of(FoodCategory.KOREAN), CompanionType.DATE, latitude, 127.0);

        assertThat(propertyPaths(request)).containsExactly("latitude");
    }

    @ParameterizedTest
    @CsvSource({"-181.0", "181.0"})
    void 경도가_범위를_벗어나면_위반된다(double longitude) {
        RecommendationRequest request = new RecommendationRequest(
                Set.of(FoodCategory.KOREAN), CompanionType.DATE, 37.5, longitude);

        assertThat(propertyPaths(request)).containsExactly("longitude");
    }

    private static Set<String> propertyPaths(RecommendationRequest request) {
        Set<ConstraintViolation<RecommendationRequest>> violations = validator.validate(request);
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
