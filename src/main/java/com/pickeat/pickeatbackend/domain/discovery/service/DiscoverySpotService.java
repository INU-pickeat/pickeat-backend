package com.pickeat.pickeatbackend.domain.discovery.service;

import com.pickeat.pickeatbackend.domain.discovery.dto.DiscoverySpotsResponse;
import com.pickeat.pickeatbackend.domain.discovery.repository.DiscoverySpotRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiscoverySpotService {

    private final DiscoverySpotRepository discoverySpotRepository;

    @Transactional(readOnly = true)
    public DiscoverySpotsResponse getDiscoverySpots() {
        // 한줄평은 사용자 후기 기반이다. Review 모듈과 집계 방식이 정해지기 전까지는 후기가 없으므로
        // 빈 맵을 넘겨 모든 식당이 "후기가 없습니다."로 내려간다. 집계가 정해지면 이 맵만 채우면 된다.
        Map<Long, String> oneLineReviews = Map.of();
        return DiscoverySpotsResponse.from(discoverySpotRepository.findAllByOrderByDisplayOrderAsc(), oneLineReviews);
    }
}
