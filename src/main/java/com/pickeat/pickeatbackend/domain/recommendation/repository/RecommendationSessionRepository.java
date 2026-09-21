package com.pickeat.pickeatbackend.domain.recommendation.repository;

import com.pickeat.pickeatbackend.domain.recommendation.entity.RecommendationSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationSessionRepository extends JpaRepository<RecommendationSession, Long> {

    Optional<RecommendationSession> findByIdAndMemberId(Long id, Long memberId);
}
