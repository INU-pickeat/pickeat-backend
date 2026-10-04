package com.pickeat.pickeatbackend.domain.discovery.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "discovery_spot_restaurants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiscoverySpotRestaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discovery_spot_id", nullable = false)
    private DiscoverySpot discoverySpot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false)
    private Integer displayOrder;

    // 운영자가 시드한 소개 문구. 한줄평을 사용자 후기 기반으로 바꾸면서 API 응답에는 더 이상 쓰지 않는다.
    @Column(nullable = false, length = 200)
    private String oneLineIntro;
}
