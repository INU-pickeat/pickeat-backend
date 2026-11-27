package com.pickeat.pickeatbackend.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.admin.dto.AdminSystemStatus;
import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlacesClient;
import com.pickeat.pickeatbackend.global.exception.RecentErrors;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminSystemServiceTest {

    private final MeterRegistry registry = new SimpleMeterRegistry();
    private final RecentErrors recentErrors = new RecentErrors();
    private final AdminSystemService service = new AdminSystemService(registry, recentErrors);

    @Test
    @DisplayName("같은 경로의 상태 코드별 응답 시간을 합치고 5xx만 서버 오류로 센다")
    void mergesEndpointTimersByStatus() {
        request("GET", "/api/v1/reviews/feed", "200", 100);
        request("GET", "/api/v1/reviews/feed", "200", 300);
        request("GET", "/api/v1/reviews/feed", "500", 200);
        request("POST", "/api/v1/recommendations", "200", 50);
        request("GET", "/actuator/health", "503", 10);

        AdminSystemStatus status = service.status();

        assertThat(status.endpoints()).first().satisfies(feed -> {
            assertThat(feed.uri()).isEqualTo("/api/v1/reviews/feed");
            assertThat(feed.count()).isEqualTo(3);
            assertThat(feed.serverErrors()).isEqualTo(1);
            assertThat(feed.meanMs()).isEqualTo(200.0);
            assertThat(feed.maxMs()).isEqualTo(300.0);
        });
        assertThat(status.endpoints()).hasSize(2);
    }

    @Test
    @DisplayName("Google 호출 수에 1,000건당 단가를 곱해 예상 비용을 낸다")
    void estimatesGoogleCostFromCallCounts() {
        registry.counter(GooglePlacesClient.CALLS_METRIC, "api", GooglePlacesClient.NEARBY_SEARCH).increment(250);
        registry.counter(GooglePlacesClient.CALLS_METRIC, "api", GooglePlacesClient.PHOTO_MEDIA).increment(1000);

        AdminSystemStatus status = service.status();

        assertThat(status.googleApis())
                .filteredOn(api -> api.api().equals(GooglePlacesClient.NEARBY_SEARCH))
                .singleElement()
                .satisfies(api -> assertThat(api.estimatedUsd()).isEqualTo(10.0));
        assertThat(status.googleEstimatedUsd()).isEqualTo(17.0);
    }

    @Test
    @DisplayName("지표가 아직 없어도 빈 값으로 응답한다")
    void respondsWithoutMetrics() {
        recentErrors.record("GET", "/api/v1/x", new IllegalStateException("boom"));

        AdminSystemStatus status = service.status();

        assertThat(status.endpoints()).isEmpty();
        assertThat(status.db().active()).isNull();
        assertThat(status.googleEstimatedUsd()).isZero();
        assertThat(status.recentErrors()).hasSize(1);
        assertThat(status.jvm().heapMaxMb()).isPositive();
    }

    private void request(String method, String uri, String httpStatus, long millis) {
        Timer.builder("http.server.requests").tags("method", method, "uri", uri, "status", httpStatus)
                .register(registry).record(Duration.ofMillis(millis));
    }
}
