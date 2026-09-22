package com.pickeat.pickeatbackend.domain.recommendation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.recommendation.entity.ExclusionReason;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecommendationExclusionRequestTest {

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
        RecommendationExclusionRequest request =
                new RecommendationExclusionRequest(1L, ExclusionReason.DISTANCE_TOO_FAR);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("식당 ID가 없으면 위반된다")
    void violatesWhenRestaurantIdMissing() {
        RecommendationExclusionRequest request =
                new RecommendationExclusionRequest(null, ExclusionReason.DISTANCE_TOO_FAR);

        assertThat(propertyPaths(request)).containsExactly("restaurantId");
    }

    @Test
    @DisplayName("제외 사유가 없으면 위반된다")
    void violatesWhenReasonMissing() {
        RecommendationExclusionRequest request = new RecommendationExclusionRequest(1L, null);

        assertThat(propertyPaths(request)).containsExactly("reason");
    }

    private static Set<String> propertyPaths(RecommendationExclusionRequest request) {
        Set<ConstraintViolation<RecommendationExclusionRequest>> violations = validator.validate(request);
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
