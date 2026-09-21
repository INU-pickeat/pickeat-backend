package com.pickeat.pickeatbackend.domain.pick.repository;

import com.pickeat.pickeatbackend.domain.pick.entity.Pick;
import com.pickeat.pickeatbackend.domain.pick.entity.PickStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PickRepository extends JpaRepository<Pick, Long> {

    boolean existsByRecommendationSessionId(Long recommendationSessionId);

    @EntityGraph(attributePaths = "restaurant")
    Optional<Pick> findByIdAndMemberId(Long id, Long memberId);

    @EntityGraph(attributePaths = "restaurant")
    Page<Pick> findByMemberId(Long memberId, Pageable pageable);

    @EntityGraph(attributePaths = "restaurant")
    List<Pick> findByMemberIdAndStatusNotOrderBySelectedAtDescIdDesc(Long memberId, PickStatus status);
}
