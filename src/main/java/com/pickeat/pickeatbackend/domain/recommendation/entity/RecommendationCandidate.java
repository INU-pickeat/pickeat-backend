package com.pickeat.pickeatbackend.domain.recommendation.entity;

import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recommendation_candidates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private RecommendationSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "result_rank", nullable = false)
    private Integer resultRank;

    @Column(nullable = false)
    private Double distanceMeters;

    @Column(nullable = false)
    private Double ratingContribution;

    @Column(nullable = false)
    private Double distanceContribution;

    @Column(nullable = false)
    private Double companionBonus;

    @Column(nullable = false)
    private Double totalScore;

    @Builder
    public RecommendationCandidate(
            RecommendationSession session,
            Restaurant restaurant,
            Integer resultRank,
            Double distanceMeters,
            Double ratingContribution,
            Double distanceContribution,
            Double companionBonus,
            Double totalScore
    ) {
        this.session = session;
        this.restaurant = restaurant;
        this.resultRank = resultRank;
        this.distanceMeters = distanceMeters;
        this.ratingContribution = ratingContribution;
        this.distanceContribution = distanceContribution;
        this.companionBonus = companionBonus;
        this.totalScore = totalScore;
    }
}
