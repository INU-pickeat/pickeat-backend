package com.pickeat.pickeatbackend.domain.admin.service;

import com.pickeat.pickeatbackend.domain.admin.dto.AdminStats;
import com.pickeat.pickeatbackend.domain.restaurant.service.FranchiseBrands;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 대시보드 집계. 별도 이벤트 로그 없이 추천 세션·Pick·제외·후기 테이블을 읽는다.
 * 기간은 한국 시간 기준 오늘 포함 최근 {@code days}일이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int MAX_DAYS = 90;
    private static final int TOP_RESTAURANTS = 20;

    // 다시 추천을 누르면 세션이 새로 생긴다. Pick 직전 30분 안의 같은 회원 세션은 한 번의 결정으로 보고
    // 그중 첫 세션부터 Pick까지를 결정 시간으로 잰다.
    // ponytail: 30분 고정 창이라 30분 넘게 고민하면 마지막 30분만 잡힌다. 오차가 문제 되면 세션 간 간격으로 묶는다.
    private static final String MEDIAN_DECISION_SQL = """
            SELECT percentile_cont(0.5) WITHIN GROUP (ORDER BY extract(epoch FROM d.selected_at - d.started_at))
            FROM (
                SELECT p.selected_at,
                       (SELECT min(s2.created_at)
                        FROM recommendation_sessions s2
                        WHERE s2.member_id = p.member_id
                          AND s2.created_at BETWEEN s.created_at - interval '30 minutes' AND s.created_at) AS started_at
                FROM picks p
                JOIN recommendation_sessions s ON s.id = p.recommendation_session_id
                WHERE p.selected_at >= ?
            ) d
            """;

    private final JdbcTemplate jdbcTemplate;

    public AdminStats stats(int requestedDays) {
        int days = Math.clamp(requestedDays, 1, MAX_DAYS);
        ZonedDateTime now = ZonedDateTime.now(SEOUL);
        LocalDate firstDay = now.toLocalDate().minusDays(days - 1L);
        OffsetDateTime from = firstDay.atStartOfDay(SEOUL).toOffsetDateTime();

        List<RestaurantShown> shown = shownRestaurants(from);
        return new AdminStats(
                now.toOffsetDateTime().toString(),
                days,
                summary(from, shown),
                labelCounts("""
                        SELECT c.result_rank::text, count(*)
                        FROM picks p
                        JOIN recommendation_candidates c
                          ON c.session_id = p.recommendation_session_id AND c.restaurant_id = p.restaurant_id
                        WHERE p.selected_at >= ?
                        GROUP BY c.result_rank ORDER BY c.result_rank
                        """, from),
                labelCounts("""
                        SELECT reason, count(*) FROM recommendation_exclusions
                        WHERE excluded_at >= ? GROUP BY reason ORDER BY count(*) DESC
                        """, from),
                labelCounts("""
                        SELECT companion_type, count(*) FROM recommendation_sessions
                        WHERE created_at >= ? GROUP BY companion_type ORDER BY count(*) DESC
                        """, from),
                shown.stream()
                        .limit(TOP_RESTAURANTS)
                        .map(r -> new AdminStats.RestaurantRow(r.id(), r.name(), r.shown(), r.picked(), r.franchise()))
                        .toList(),
                daily(firstDay, now.toLocalDate())
        );
    }

    private AdminStats.Summary summary(OffsetDateTime from, List<RestaurantShown> shown) {
        Double medianDecision = jdbcTemplate.queryForObject(MEDIAN_DECISION_SQL, Double.class, from);
        return jdbcTemplate.queryForObject("""
                SELECT
                    (SELECT count(*) FROM members),
                    (SELECT count(*) FROM members WHERE created_at >= ?),
                    (SELECT count(DISTINCT member_id) FROM recommendation_sessions WHERE created_at >= ?),
                    (SELECT count(*) FROM (
                        SELECT member_id FROM recommendation_sessions WHERE created_at >= ?
                        GROUP BY member_id
                        HAVING count(DISTINCT (created_at AT TIME ZONE 'Asia/Seoul')::date) >= 2) t),
                    (SELECT count(DISTINCT member_id) FROM recommendation_sessions
                     WHERE created_at >= now() - interval '30 minutes'),
                    (SELECT count(*) FROM recommendation_sessions WHERE created_at >= ?),
                    (SELECT count(*) FROM recommendation_sessions s WHERE s.created_at >= ?
                       AND EXISTS (SELECT 1 FROM picks p WHERE p.recommendation_session_id = s.id)),
                    (SELECT count(*) FROM picks WHERE selected_at >= ?),
                    (SELECT count(*) FROM picks WHERE selected_at >= ? AND status <> 'CANCELED'),
                    (SELECT count(*) FROM picks WHERE selected_at >= ? AND status = 'REVIEWED'),
                    (SELECT count(*) FROM recommendation_exclusions WHERE excluded_at >= ?),
                    (SELECT count(*) FROM review_likes WHERE created_at >= ?)
                """, (rs, rowNum) -> new AdminStats.Summary(
                rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5), rs.getLong(6),
                rs.getLong(7), rs.getLong(8), rs.getLong(9), rs.getLong(10), rs.getLong(11), rs.getLong(12),
                medianDecision,
                shown.stream().mapToLong(RestaurantShown::shown).sum(),
                shown.stream().filter(RestaurantShown::franchise).mapToLong(RestaurantShown::shown).sum(),
                shown.stream().mapToLong(RestaurantShown::dateShown).sum(),
                shown.stream().filter(RestaurantShown::franchise).mapToLong(RestaurantShown::dateShown).sum()
        ), from, from, from, from, from, from, from, from, from, from);
    }

    // 처음 노출된 1~5위 후보만 센다. 제외 후 대체로 올라온 6~10위는 노출 여부를 따로 저장하지 않는다.
    // 프랜차이즈 여부는 DB에 없어서 추천과 같은 브랜드 목록(FranchiseBrands)으로 판정한다.
    private List<RestaurantShown> shownRestaurants(OffsetDateTime from) {
        return jdbcTemplate.query("""
                SELECT r.id, r.name, count(*) AS shown, count(p.id) AS picked,
                       count(*) FILTER (WHERE s.companion_type = 'DATE') AS date_shown
                FROM recommendation_candidates c
                JOIN recommendation_sessions s ON s.id = c.session_id
                JOIN restaurants r ON r.id = c.restaurant_id
                LEFT JOIN picks p ON p.recommendation_session_id = c.session_id AND p.restaurant_id = c.restaurant_id
                WHERE s.created_at >= ? AND c.result_rank <= 5
                GROUP BY r.id, r.name
                ORDER BY shown DESC, picked DESC, r.id
                """, (rs, rowNum) -> new RestaurantShown(
                rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getLong(5),
                FranchiseBrands.isFranchise(rs.getString(2))
        ), from);
    }

    private List<AdminStats.Daily> daily(LocalDate firstDay, LocalDate today) {
        return jdbcTemplate.query("""
                SELECT to_char(g.day, 'YYYY-MM-DD'),
                    (SELECT count(*) FROM recommendation_sessions
                     WHERE (created_at AT TIME ZONE 'Asia/Seoul')::date = g.day::date),
                    (SELECT count(DISTINCT member_id) FROM recommendation_sessions
                     WHERE (created_at AT TIME ZONE 'Asia/Seoul')::date = g.day::date),
                    (SELECT count(*) FROM picks
                     WHERE (selected_at AT TIME ZONE 'Asia/Seoul')::date = g.day::date),
                    (SELECT count(*) FROM reviews
                     WHERE (created_at AT TIME ZONE 'Asia/Seoul')::date = g.day::date)
                FROM generate_series(CAST(? AS date), CAST(? AS date), interval '1 day') AS g(day)
                ORDER BY g.day
                """, (rs, rowNum) -> new AdminStats.Daily(
                rs.getString(1), rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5)
        ), firstDay, today);
    }

    private List<AdminStats.LabelCount> labelCounts(String sql, Object... args) {
        return jdbcTemplate.query(sql, AdminStatsService::toLabelCount, args);
    }

    private static AdminStats.LabelCount toLabelCount(ResultSet rs, int rowNum) throws SQLException {
        return new AdminStats.LabelCount(rs.getString(1), rs.getLong(2));
    }

    private record RestaurantShown(long id, String name, long shown, long picked, long dateShown, boolean franchise) {
    }
}
