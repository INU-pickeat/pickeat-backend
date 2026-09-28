package com.pickeat.pickeatbackend.domain.discovery.controller;

import com.pickeat.pickeatbackend.domain.discovery.dto.DiscoverySpotsResponse;
import com.pickeat.pickeatbackend.domain.discovery.service.DiscoverySpotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/discovery-spots")
@RequiredArgsConstructor
public class DiscoverySpotController {

    private final DiscoverySpotService discoverySpotService;

    @GetMapping
    public ResponseEntity<DiscoverySpotsResponse> getDiscoverySpots() {
        return ResponseEntity.ok(discoverySpotService.getDiscoverySpots());
    }
}
