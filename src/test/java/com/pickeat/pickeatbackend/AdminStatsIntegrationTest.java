package com.pickeat.pickeatbackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pickeat.pickeatbackend.domain.admin.dto.AdminStats;
import com.pickeat.pickeatbackend.domain.admin.service.AdminStatsService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// 관리자 집계 SQL을 실제 DB로 검증한다. 로컬 DB에 다른 데이터가 있을 수 있어 넣기 전후 차이로 확인한다.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminStatsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AdminStatsService adminStatsService;

    @Test
    @DisplayName("관리자 API와 화면은 관리자 Basic 인증이 있어야 열린다")
    void requiresAdminBasicAuth() throws Exception {
        mockMvc.perform(get("/api/admin/stats")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/admin.html")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/stats").with(httpBasic("admin", "wrong")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/stats").with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin.html").with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("재추천 세션은 Pick 전환에서 빠지고, Pick한 순위·제외 사유·데이트 프랜차이즈 노출을 센다")
    void aggregatesRecommendationFunnel() {
        AdminStats before = adminStatsService.stats(7);

        Long memberId = insertMember();
        Long local = insertRestaurant("동네 국밥집");
        Long franchise = insertRestaurant("스타벅스 테스트점");
        Long retried = insertSession(memberId, "DATE", "10 minutes");
        insertCandidate(retried, local, 1);
        jdbcTemplate.update("""
                INSERT INTO recommendation_exclusions (session_id, restaurant_id, reason)
                VALUES (?, ?, 'DISTANCE_TOO_FAR')
                """, retried, local);
        Long picked = insertSession(memberId, "DATE", "5 minutes");
        insertCandidate(picked, franchise, 1);
        insertCandidate(picked, local, 2);
        jdbcTemplate.update("""
                INSERT INTO picks (member_id, restaurant_id, recommendation_session_id, companion_type)
                VALUES (?, ?, ?, 'DATE')
                """, memberId, local, picked);

        AdminStats after = adminStatsService.stats(7);
        AdminStats.Summary b = before.summary();
        AdminStats.Summary a = after.summary();
        assertThat(a.sessions() - b.sessions()).isEqualTo(2);
        assertThat(a.pickedSessions() - b.pickedSessions()).isEqualTo(1);
        assertThat(a.activePicks() - b.activePicks()).isEqualTo(1);
        assertThat(a.exclusions() - b.exclusions()).isEqualTo(1);
        assertThat(a.dateShownCandidates() - b.dateShownCandidates()).isEqualTo(3);
        assertThat(a.dateShownFranchise() - b.dateShownFranchise()).isEqualTo(1);
        assertThat(a.medianDecisionSeconds()).isNotNull();
        assertThat(count(after.pickedRanks(), "2") - count(before.pickedRanks(), "2")).isEqualTo(1);
        assertThat(count(after.exclusionReasons(), "DISTANCE_TOO_FAR")
                - count(before.exclusionReasons(), "DISTANCE_TOO_FAR")).isEqualTo(1);
        assertThat(after.daily()).hasSize(7);
    }

    @Test
    @DisplayName("재요청을 묶어 첫 추천 세션부터 Pick까지를 결정 시간으로 잰다")
    void measuresDecisionFromFirstRetriedSession() {
        // 중앙값은 차이로 볼 수 없어서 기간 안의 기존 Pick을 지운다(테스트 트랜잭션이라 롤백된다).
        jdbcTemplate.update("""
                DELETE FROM reviews WHERE pick_id IN (
                    SELECT id FROM picks WHERE selected_at >= now() - interval '8 days')
                """);
        jdbcTemplate.update("DELETE FROM picks WHERE selected_at >= now() - interval '8 days'");
        Long memberId = insertMember();
        Long restaurant = insertRestaurant("결정 시간 식당");
        insertSession(memberId, "SOLO", "10 minutes");
        Long picked = insertSession(memberId, "SOLO", "4 minutes");
        insertCandidate(picked, restaurant, 1);
        jdbcTemplate.update("""
                INSERT INTO picks (member_id, restaurant_id, recommendation_session_id, companion_type)
                VALUES (?, ?, ?, 'SOLO')
                """, memberId, restaurant, picked);

        // 트랜잭션 안의 now()는 고정이라 Pick 시각 - 첫 세션 시각은 정확히 10분이다.
        assertThat(adminStatsService.stats(7).summary().medianDecisionSeconds()).isEqualTo(600.0);
    }

    private static long count(List<AdminStats.LabelCount> counts, String label) {
        return counts.stream().filter(c -> c.label().equals(label)).mapToLong(AdminStats.LabelCount::count).sum();
    }

    private Long insertMember() {
        return jdbcTemplate.queryForObject("""
                INSERT INTO members (email, password, nickname, login_provider, created_at, updated_at)
                VALUES (?, 'password', '테스터', 'LOCAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """, Long.class, "admin-stats-" + System.nanoTime() + "@pickeat.com");
    }

    private Long insertRestaurant(String name) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO restaurants (name, food_category, latitude, longitude)
                VALUES (?, 'KOREAN', 37.58, 127.0)
                RETURNING id
                """, Long.class, name);
    }

    private Long insertSession(Long memberId, String companionType, String age) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO recommendation_sessions (member_id, companion_type, latitude, longitude, created_at)
                VALUES (?, ?, 37.58, 127.0, CURRENT_TIMESTAMP - CAST(? AS INTERVAL))
                RETURNING id
                """, Long.class, memberId, companionType, age);
    }

    private void insertCandidate(Long sessionId, Long restaurantId, int rank) {
        jdbcTemplate.update("""
                INSERT INTO recommendation_candidates (session_id, restaurant_id, result_rank, distance_meters,
                    rating_contribution, distance_contribution, companion_bonus, total_score)
                VALUES (?, ?, ?, 100, 0.5, 0.3, 0, 0.8)
                """, sessionId, restaurantId, rank);
    }
}
