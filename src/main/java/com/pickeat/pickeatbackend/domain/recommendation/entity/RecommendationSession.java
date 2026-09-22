package com.pickeat.pickeatbackend.domain.recommendation.entity;

import com.pickeat.pickeatbackend.domain.member.entity.Member;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recommendation_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanionType companionType;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(precision = 19, scale = 2)
    private BigDecimal priceRangeMin;

    @Column(precision = 19, scale = 2)
    private BigDecimal priceRangeMax;

    @ElementCollection
    @CollectionTable(name = "recommendation_session_food_categories", joinColumns = @JoinColumn(name = "session_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "food_category", nullable = false, length = 30)
    private Set<FoodCategory> foodCategories = new LinkedHashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public RecommendationSession(
            Member member,
            CompanionType companionType,
            Double latitude,
            Double longitude,
            BigDecimal priceRangeMin,
            BigDecimal priceRangeMax,
            Set<FoodCategory> foodCategories
    ) {
        this.member = member;
        this.companionType = companionType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.priceRangeMin = priceRangeMin;
        this.priceRangeMax = priceRangeMax;
        this.foodCategories = new LinkedHashSet<>(foodCategories);
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
