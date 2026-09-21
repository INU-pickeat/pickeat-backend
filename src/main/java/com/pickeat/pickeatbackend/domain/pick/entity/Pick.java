package com.pickeat.pickeatbackend.domain.pick.entity;

import com.pickeat.pickeatbackend.domain.member.entity.Member;
import com.pickeat.pickeatbackend.domain.pick.exception.PickErrorCode;
import com.pickeat.pickeatbackend.domain.recommendation.entity.RecommendationSession;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "picks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_session_id", nullable = false, unique = true)
    private RecommendationSession recommendationSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PickStatus status;

    @Column(nullable = false, updatable = false)
    private Instant selectedAt;

    private Instant visitedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Builder
    public Pick(Member member, Restaurant restaurant, RecommendationSession recommendationSession) {
        this.member = member;
        this.restaurant = restaurant;
        this.recommendationSession = recommendationSession;
        this.status = PickStatus.SELECTED;
    }

    public void changeStatus(PickStatus nextStatus) {
        if (status == nextStatus) {
            return;
        }
        if (status != PickStatus.SELECTED
                || (nextStatus != PickStatus.REVIEWED && nextStatus != PickStatus.CANCELED)) {
            throw new BusinessException(PickErrorCode.INVALID_STATUS_TRANSITION);
        }
        status = nextStatus;
        if (nextStatus == PickStatus.REVIEWED) {
            visitedAt = Instant.now();
        }
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        selectedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
