package com.pickeat.pickeatbackend.domain.admin.dto;

import java.util.List;

/**
 * GET /api/admin/stats 응답. 사용자 테스트 지표(결정 시간·Pick 전환율·추천 품질·기록 사용률)를
 * 기존 테이블에서 집계한다. 비율은 화면에서 계산하도록 원본 건수만 담는다.
 */
public record AdminStats(
        String generatedAt,
        int days,
        Summary summary,
        List<LabelCount> pickedRanks,
        List<LabelCount> exclusionReasons,
        List<LabelCount> sessionsByCompanion,
        List<RestaurantRow> topRecommended,
        List<Daily> daily
) {

    public record Summary(
            long totalMembers,
            long newMembers,
            long activeMembers,
            long returningMembers,
            long activeNow,
            long sessions,
            long pickedSessions,
            long picks,
            long activePicks,
            long reviewedPicks,
            long exclusions,
            long likes,
            Double medianDecisionSeconds,
            long shownCandidates,
            long shownFranchise,
            long dateShownCandidates,
            long dateShownFranchise
    ) {
    }

    public record LabelCount(String label, long count) {
    }

    public record RestaurantRow(long restaurantId, String name, long shown, long picked, boolean franchise) {
    }

    public record Daily(String date, long sessions, long activeMembers, long picks, long reviews) {
    }
}
