package com.pickeat.pickeatbackend.domain.admin.dto;

import com.pickeat.pickeatbackend.global.exception.RecentErrors;
import java.util.List;

/** 관리자 운영 상태. 느린 쿼리를 뺀 값은 이번 기동 이후 서버 메모리 기준이며 재시작하면 0부터 다시 센다. */
public record AdminSystemStatus(
        String generatedAt,
        String startedAt,
        long uptimeSeconds,
        Jvm jvm,
        Db db,
        List<Endpoint> endpoints,
        List<GoogleApi> googleApis,
        double googleEstimatedUsd,
        List<RecentErrors.Entry> recentErrors,
        SlowQueries slowQueries
) {

    /** enabled=false면 DB에 pg_stat_statements가 올라가 있지 않다. 이 값만 DB 기준이라 앱 재시작과 상관없이 쌓인다. */
    public record SlowQueries(boolean enabled, List<SlowQuery> top) {
    }

    /** 시간 단위는 ms. 쿼리 문장은 공백을 합쳐 300자까지, 바인딩 값은 $1처럼 가려진다. */
    public record SlowQuery(String query, long calls, double meanMs, double maxMs, double totalMs, long rows) {
    }

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
