package com.pickeat.pickeatbackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PickSchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Pick 마이그레이션과 목록 인덱스가 적용된다")
    void appliesPickMigrationAndIndex() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '8'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT indexdef FROM pg_indexes
                WHERE schemaname = 'public' AND indexname = 'picks_member_selected_idx'
                """, String.class)).contains("member_id", "selected_at DESC", "id DESC");
    }

    @Test
    @DisplayName("하나의 추천 세션에는 Pick을 하나만 저장할 수 있다")
    void rejectsDuplicatePickForRecommendationSession() {
        TestIds ids = insertDependencies();
        insertPick(ids);

        assertThatThrownBy(() -> insertPick(ids))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("정의되지 않은 Pick 상태는 저장할 수 없다")
    void rejectsUnknownStatus() {
        TestIds ids = insertDependencies();

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO picks (member_id, restaurant_id, recommendation_session_id, status)
                VALUES (?, ?, ?, 'UNKNOWN')
                """, ids.memberId(), ids.restaurantId(), ids.sessionId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private TestIds insertDependencies() {
        Long memberId = jdbcTemplate.queryForObject("""
                INSERT INTO members (email, password, nickname, login_provider, created_at, updated_at)
                VALUES (?, 'password', '테스터', 'LOCAL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """, Long.class, "pick-" + System.nanoTime() + "@pickeat.com");
        Long restaurantId = jdbcTemplate.queryForObject("""
                INSERT INTO restaurants (name, food_category, latitude, longitude)
                VALUES ('Pick 테스트 식당', 'KOREAN', 37.58, 127.0)
                RETURNING id
                """, Long.class);
        Long sessionId = jdbcTemplate.queryForObject("""
                INSERT INTO recommendation_sessions (member_id, companion_type, latitude, longitude)
                VALUES (?, 'DATE', 37.58, 127.0)
                RETURNING id
                """, Long.class, memberId);
        return new TestIds(memberId, restaurantId, sessionId);
    }

    private void insertPick(TestIds ids) {
        jdbcTemplate.update("""
                INSERT INTO picks (member_id, restaurant_id, recommendation_session_id)
                VALUES (?, ?, ?)
                """, ids.memberId(), ids.restaurantId(), ids.sessionId());
    }

    private record TestIds(Long memberId, Long restaurantId, Long sessionId) {
    }
}
