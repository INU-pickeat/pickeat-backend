package com.pickeat.pickeatbackend.domain.discovery.service;

import com.pickeat.pickeatbackend.domain.discovery.dto.DiscoverySpotsResponse;
import com.pickeat.pickeatbackend.domain.discovery.repository.DiscoverySpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiscoverySpotService {

    private final DiscoverySpotRepository discoverySpotRepository;

    @Transactional(readOnly = true)
    public DiscoverySpotsResponse getDiscoverySpots() {
        return DiscoverySpotsResponse.from(discoverySpotRepository.findAllByOrderByDisplayOrderAsc());
    }
}
