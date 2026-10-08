package com.pickeat.pickeatbackend.domain.restaurant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 사용자가 남긴 폐업·정보 오류 신고. 검토(ACCEPTED·REJECTED)는 운영자가 SQL로 처리한다.
@Entity
@Table(name = "restaurant_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestaurantReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long memberId;

    @Column(nullable = false, updatable = false)
    private Long restaurantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private RestaurantReportReason reason;

    @Column(length = 300, updatable = false)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RestaurantReportStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public RestaurantReport(Long memberId, Long restaurantId, RestaurantReportReason reason, String detail) {
        this.memberId = memberId;
        this.restaurantId = restaurantId;
        this.reason = reason;
        this.detail = detail;
        this.status = RestaurantReportStatus.PENDING;
        this.createdAt = Instant.now();
    }
}
