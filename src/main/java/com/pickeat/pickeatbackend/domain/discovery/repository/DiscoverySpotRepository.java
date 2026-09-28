package com.pickeat.pickeatbackend.domain.discovery.repository;

import com.pickeat.pickeatbackend.domain.discovery.entity.DiscoverySpot;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscoverySpotRepository extends JpaRepository<DiscoverySpot, Long> {

    @EntityGraph(attributePaths = {"restaurants", "restaurants.restaurant"})
    List<DiscoverySpot> findAllByOrderByDisplayOrderAsc();
}
