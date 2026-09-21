package com.pickeat.pickeatbackend.domain.recommendation.repository;

import com.pickeat.pickeatbackend.domain.recommendation.entity.RecommendationCandidate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationCandidateRepository extends JpaRepository<RecommendationCandidate, Long> {

    List<RecommendationCandidate> findBySessionIdOrderByResultRankAsc(Long sessionId);

    boolean existsBySessionIdAndRestaurantId(Long sessionId, Long restaurantId);
}
