package com.pickeat.pickeatbackend.domain.restaurant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "restaurants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(length = 50)
    private String foodCategory;

    private Boolean suitableForDate;
    private Boolean suitableForFriends;
    private Boolean suitableForFamily;
    private Boolean suitableForSolo;
    private Boolean suitableForGroupDinner;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    // location은 DB 생성 컬럼이다. JPA에서는 원본 위도·경도만 저장한다.
    @Builder
    private Restaurant(String name, String address, Double latitude, Double longitude,
                       String foodCategory, Boolean suitableForDate, Boolean suitableForFriends,
                       Boolean suitableForFamily, Boolean suitableForSolo,
                       Boolean suitableForGroupDinner) {
        this.name = Objects.requireNonNull(name, "식당명은 필수입니다.");
        this.address = address;
        this.latitude = Objects.requireNonNull(latitude, "위도는 필수입니다.");
        this.longitude = Objects.requireNonNull(longitude, "경도는 필수입니다.");
        this.foodCategory = foodCategory;
        this.suitableForDate = suitableForDate;
        this.suitableForFriends = suitableForFriends;
        this.suitableForFamily = suitableForFamily;
        this.suitableForSolo = suitableForSolo;
        this.suitableForGroupDinner = suitableForGroupDinner;
    }

    @PrePersist
    private void initializeTimestamps() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void refreshUpdatedAt() {
        this.updatedAt = Instant.now();
    }
}
