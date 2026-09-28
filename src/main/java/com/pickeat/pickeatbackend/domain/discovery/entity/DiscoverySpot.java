package com.pickeat.pickeatbackend.domain.discovery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "discovery_spots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiscoverySpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "region_code", nullable = false, unique = true, length = 30)
    private DiscoveryRegion regionCode;

    @Column(nullable = false, length = 50)
    private String regionName;

    @Column(nullable = false)
    private Integer displayOrder;

    @OneToMany(mappedBy = "discoverySpot", fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<DiscoverySpotRestaurant> restaurants = new ArrayList<>();
}
