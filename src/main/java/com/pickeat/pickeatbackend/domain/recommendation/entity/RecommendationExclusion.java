package com.pickeat.pickeatbackend.domain.recommendation.entity;

import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 제외는 해당 세션 안에서만 유효하다(영구 차단 아님). 다른 세션·이후 추천에는 영향을 주지 않는다.
@Entity
@Table(name = "recommendation_exclusions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationExclusion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private RecommendationSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExclusionReason reason;

    @Column(nullable = false, updatable = false)
    private Instant excludedAt;

    @Builder
    public RecommendationExclusion(RecommendationSession session, Restaurant restaurant, ExclusionReason reason) {
        this.session = session;
        this.restaurant = restaurant;
        this.reason = reason;
    }

    @PrePersist
    protected void onCreate() {
        this.excludedAt = Instant.now();
    }
}
