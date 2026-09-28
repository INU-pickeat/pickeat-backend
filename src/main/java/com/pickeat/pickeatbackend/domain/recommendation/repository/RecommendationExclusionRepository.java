package com.pickeat.pickeatbackend.domain.recommendation.repository;

import com.pickeat.pickeatbackend.domain.recommendation.entity.RecommendationExclusion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationExclusionRepository extends JpaRepository<RecommendationExclusion, Long> {

    List<RecommendationExclusion> findBySessionId(Long sessionId);

    boolean existsBySessionIdAndRestaurantId(Long sessionId, Long restaurantId);
}
