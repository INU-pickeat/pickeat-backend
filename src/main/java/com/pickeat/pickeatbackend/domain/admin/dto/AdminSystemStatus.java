package com.pickeat.pickeatbackend.domain.admin.dto;

import com.pickeat.pickeatbackend.global.exception.RecentErrors;
import java.util.List;

/** 관리자 운영 상태. 값은 모두 이번 기동 이후 서버 메모리 기준이며 재시작하면 0부터 다시 센다. */
public record AdminSystemStatus(
        String generatedAt,
        String startedAt,
        long uptimeSeconds,
        Jvm jvm,
        Db db,
        List<Endpoint> endpoints,
        List<GoogleApi> googleApis,
        double googleEstimatedUsd,
        List<RecentErrors.Entry> recentErrors
) {

    public record Jvm(long heapUsedMb, long heapMaxMb) {
    }

    /** Hikari 커넥션 풀. 지표가 아직 없으면 null. */
    public record Db(Integer active, Integer idle, Integer pending, Integer max) {
    }

    /** maxMs는 Micrometer 기준 최근 몇 분 안의 최댓값이다. */
    public record Endpoint(String method, String uri, long count, long serverErrors, double meanMs, double maxMs) {
    }

    /** estimatedUsd는 호출 수 × 단가 단순 환산이며 월 무료 한도를 빼지 않는다. */
    public record GoogleApi(String api, String label, long calls, double unitPriceUsd, double estimatedUsd) {
    }
}
