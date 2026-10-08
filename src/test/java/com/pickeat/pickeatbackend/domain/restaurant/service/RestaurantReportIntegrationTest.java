package com.pickeat.pickeatbackend.domain.restaurant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pickeat.pickeatbackend.domain.restaurant.dto.RestaurantReportRequest;
import com.pickeat.pickeatbackend.domain.restaurant.dto.RestaurantReportResponse;
import com.pickeat.pickeatbackend.domain.restaurant.entity.RestaurantReportReason;
import com.pickeat.pickeatbackend.domain.restaurant.entity.RestaurantReportStatus;
import com.pickeat.pickeatbackend.domain.restaurant.exception.RestaurantErrorCode;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantCandidate;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RestaurantReportIntegrationTest {

    private static final double LATITUDE = 35.1;
    private static final double LONGITUDE = 129.0;

    @Autowired RestaurantReportService restaurantReportService;
    @Autowired RestaurantRepository restaurantRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private Long memberId;
    private Long restaurantId;

    @BeforeEach
    void setUp() {
        memberId = jdbcTemplate.queryForObject("""
                INSERT INTO members (email, password, nickname, created_at, updated_at)
                VALUES ('report@example.com', 'encoded', 'report', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """, Long.class);
        restaurantId = jdbcTemplate.queryForObject("""
                INSERT INTO restaurants (name, food_category, latitude, longitude)
                VALUES ('신고 테스트 식당', 'KOREAN', ?, ?)
                RETURNING id
                """, Long.class, LATITUDE, LONGITUDE);
    }

    @Test
    @DisplayName("신고하면 PENDING으로 저장되고 상세 내용은 앞뒤 공백을 지운다")
    void storesPendingReport() {
        RestaurantReportResponse response = restaurantReportService.report(
                restaurantId, new RestaurantReportRequest(RestaurantReportReason.CLOSED, "  문 닫았어요  "), memberId);

        assertThat(response.status()).isEqualTo(RestaurantReportStatus.PENDING);
        assertThat(response.reason()).isEqualTo(RestaurantReportReason.CLOSED);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT detail FROM restaurant_reports WHERE id = ?", String.class, response.reportId()))
                .isEqualTo("문 닫았어요");
    }

    @Test
    @DisplayName("검토 전에 같은 식당을 다시 신고하면 409 RESTAURANT_003")
    void rejectsDuplicatePendingReport() {
        RestaurantReportRequest request = new RestaurantReportRequest(RestaurantReportReason.WRONG_INFO, null);
        restaurantReportService.report(restaurantId, request, memberId);

        assertThatThrownBy(() -> restaurantReportService.report(restaurantId, request, memberId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(RestaurantErrorCode.REPORT_ALREADY_PENDING.getMessage());
    }

    @Test
    @DisplayName("검토가 끝난 뒤에는 같은 식당을 다시 신고할 수 있다")
    void allowsReportAfterReview() {
        RestaurantReportRequest request = new RestaurantReportRequest(RestaurantReportReason.CLOSED, null);
        RestaurantReportResponse first = restaurantReportService.report(restaurantId, request, memberId);
        jdbcTemplate.update("UPDATE restaurant_reports SET status = 'REJECTED', reviewed_at = now() WHERE id = ?",
                first.reportId());

        assertThat(restaurantReportService.report(restaurantId, request, memberId).status())
                .isEqualTo(RestaurantReportStatus.PENDING);
    }

    @Test
    @DisplayName("없는 식당 신고는 404 RESTAURANT_001")
    void rejectsUnknownRestaurant() {
        assertThatThrownBy(() -> restaurantReportService.report(
                Long.MAX_VALUE, new RestaurantReportRequest(RestaurantReportReason.CLOSED, null), memberId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(RestaurantErrorCode.RESTAURANT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("운영자가 제외 처리한 식당은 추천 후보 조회에서 빠진다")
    void excludedRestaurantIsNotACandidate() {
        assertThat(candidateNames()).contains("신고 테스트 식당");

        jdbcTemplate.update("UPDATE restaurants SET excluded_at = now(), excluded_reason = '폐업 확인' WHERE id = ?",
                restaurantId);

        assertThat(candidateNames()).doesNotContain("신고 테스트 식당");
    }

    private List<String> candidateNames() {
        return restaurantRepository.findWithinRadius(LATITUDE, LONGITUDE, 1000).stream()
                .map(RestaurantCandidate::restaurant)
                .map(r -> r.getName())
                .toList();
    }
}
