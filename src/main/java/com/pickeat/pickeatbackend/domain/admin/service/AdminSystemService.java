package com.pickeat.pickeatbackend.domain.admin.service;

import static com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlacesClient.CALLS_METRIC;
import static com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlacesClient.CONTACT_DETAILS;
import static com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlacesClient.NEARBY_SEARCH;
import static com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlacesClient.PHOTO_DETAILS;
import static com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlacesClient.PHOTO_MEDIA;

import com.pickeat.pickeatbackend.domain.admin.dto.AdminSystemStatus;
import com.pickeat.pickeatbackend.global.exception.RecentErrors;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 관리자 운영 상태. 별도 모니터링 서버 없이 앱 안에 쌓이는 Micrometer 지표와 최근 오류를 읽는다.
 * ponytail: 재시작하면 0부터 센다. 기간 비교·알림이 필요하면 외부 수집기가 /actuator/prometheus를 읽게 한다.
 */
@Service
@RequiredArgsConstructor
public class AdminSystemService {

    private static final int TOP_ENDPOINTS = 20;

    // Places API (New) 1,000건당 단가(USD). docs/domains/recommendation.md "단가" 참고.
    // 사진 이름 조회(photos 필드만 요청)는 Place Details Essentials (IDs Only) 등급이라 0으로 본다.
    private static final List<GoogleSku> GOOGLE_SKUS = List.of(
            new GoogleSku(NEARBY_SEARCH, "Nearby Search Enterprise + Atmosphere (추천 보충)", 40.0),
            new GoogleSku(PHOTO_DETAILS, "Place Details IDs Only (사진 이름)", 0.0),
            new GoogleSku(PHOTO_MEDIA, "Place Photo (사진 주소)", 7.0),
            new GoogleSku(CONTACT_DETAILS, "Place Details Enterprise (연락처 백필)", 20.0)
    );

    private final MeterRegistry meterRegistry;
    private final RecentErrors recentErrors;

    public AdminSystemStatus status() {
        Instant now = Instant.now();
        Instant startedAt = Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime());
        Runtime runtime = Runtime.getRuntime();
        List<AdminSystemStatus.GoogleApi> googleApis = googleApis();
        return new AdminSystemStatus(
                now.toString(),
                startedAt.toString(),
                Duration.between(startedAt, now).toSeconds(),
                new AdminSystemStatus.Jvm(
                        (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024, runtime.maxMemory() / 1024 / 1024),
                new AdminSystemStatus.Db(
                        gauge("hikaricp.connections.active"), gauge("hikaricp.connections.idle"),
                        gauge("hikaricp.connections.pending"), gauge("hikaricp.connections.max")),
                endpoints(),
                googleApis,
                round(googleApis.stream().mapToDouble(AdminSystemStatus.GoogleApi::estimatedUsd).sum()),
                recentErrors.list()
        );
    }

    // 같은 경로라도 상태 코드별로 타이머가 따로 있어 메서드+경로로 합친다.
    private List<AdminSystemStatus.Endpoint> endpoints() {
        Map<String, EndpointSum> sums = new LinkedHashMap<>();
        for (Timer timer : meterRegistry.find("http.server.requests").timers()) {
            String method = timer.getId().getTag("method");
            String uri = timer.getId().getTag("uri");
            if (uri == null || uri.startsWith("/actuator")) {
                continue; // 배포 헬스체크·지표 수집 요청은 사용자 트래픽이 아니다.
            }
            String status = timer.getId().getTag("status");
            EndpointSum sum = sums.computeIfAbsent(method + " " + uri, key -> new EndpointSum(method, uri));
            sum.count += timer.count();
            sum.totalMs += timer.totalTime(TimeUnit.MILLISECONDS);
            sum.maxMs = Math.max(sum.maxMs, timer.max(TimeUnit.MILLISECONDS));
            if (status != null && status.startsWith("5")) {
                sum.serverErrors += timer.count();
            }
        }
        return sums.values().stream()
                .sorted(Comparator.comparingLong((EndpointSum sum) -> sum.count).reversed())
                .limit(TOP_ENDPOINTS)
                .map(sum -> new AdminSystemStatus.Endpoint(sum.method, sum.uri, sum.count, sum.serverErrors,
                        round(sum.count == 0 ? 0 : sum.totalMs / sum.count), round(sum.maxMs)))
                .toList();
    }

    private List<AdminSystemStatus.GoogleApi> googleApis() {
        return GOOGLE_SKUS.stream().map(sku -> {
            Counter counter = meterRegistry.find(CALLS_METRIC).tag("api", sku.api()).counter();
            long calls = counter == null ? 0 : (long) counter.count();
            return new AdminSystemStatus.GoogleApi(sku.api(), sku.label(), calls, sku.pricePer1000(),
                    round(calls * sku.pricePer1000() / 1000));
        }).toList();
    }

    private Integer gauge(String name) {
        Gauge gauge = meterRegistry.find(name).gauge();
        return gauge == null ? null : (int) gauge.value();
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }

    private record GoogleSku(String api, String label, double pricePer1000) {
    }

    private static final class EndpointSum {
        private final String method;
        private final String uri;
        private long count;
        private long serverErrors;
        private double totalMs;
        private double maxMs;

        private EndpointSum(String method, String uri) {
            this.method = method;
            this.uri = uri;
        }
    }
}
