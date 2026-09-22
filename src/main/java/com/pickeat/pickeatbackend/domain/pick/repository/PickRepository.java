package com.pickeat.pickeatbackend.domain.pick.repository;

import com.pickeat.pickeatbackend.domain.pick.entity.Pick;
import com.pickeat.pickeatbackend.domain.pick.entity.PickStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PickRepository extends JpaRepository<Pick, Long> {

    boolean existsByRecommendationSessionId(Long recommendationSessionId);

    @EntityGraph(attributePaths = "restaurant")
    Optional<Pick> findByIdAndMemberId(Long id, Long memberId);

    @EntityGraph(attributePaths = "restaurant")
    List<Pick> findByMemberIdAndStatusOrderBySelectedAtDescIdDesc(Long memberId, PickStatus status);

    // 최근 Pick 목록(홈·내 정보): SELECTED + REVIEWED만 집계 대상이며 CANCELED는 제외한다.
    @Query("""
            SELECT new com.pickeat.pickeatbackend.domain.pick.repository.RestaurantPickSummary(
                p.restaurant.id, p.restaurant.name, COUNT(p), MAX(p.selectedAt))
            FROM Pick p
            WHERE p.member.id = :memberId
              AND p.status <> com.pickeat.pickeatbackend.domain.pick.entity.PickStatus.CANCELED
              AND p.selectedAt >= :since
            GROUP BY p.restaurant.id, p.restaurant.name
            ORDER BY MAX(p.selectedAt) DESC
            """)
    List<RestaurantPickSummary> findRecentPickSummaries(@Param("memberId") Long memberId, @Param("since") Instant since);
}
