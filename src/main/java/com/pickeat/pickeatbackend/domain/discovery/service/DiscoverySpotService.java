package com.pickeat.pickeatbackend.domain.discovery.service;

import com.pickeat.pickeatbackend.domain.discovery.dto.DiscoverySpotsResponse;
import com.pickeat.pickeatbackend.domain.discovery.entity.DiscoverySpot;
import com.pickeat.pickeatbackend.domain.discovery.repository.DiscoverySpotRepository;
import com.pickeat.pickeatbackend.domain.review.service.ReviewSummaryService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiscoverySpotService {

    private final DiscoverySpotRepository discoverySpotRepository;
    private final ReviewSummaryService reviewSummaryService;

    @Transactional(readOnly = true)
    public DiscoverySpotsResponse getDiscoverySpots() {
        List<DiscoverySpot> spots = discoverySpotRepository.findAllByOrderByDisplayOrderAsc();
        // 한줄평은 식당별 가장 최근 공개 후기에서 가져온다. 후기가 없는 식당은 "후기가 없습니다."로 내려간다.
        List<Long> restaurantIds = spots.stream()
                .flatMap(spot -> spot.getRestaurants().stream())
                .map(item -> item.getRestaurant().getId())
                .toList();
        Map<Long, String> oneLineReviews = reviewSummaryService.getOneLineReviews(restaurantIds);
        return DiscoverySpotsResponse.from(spots, oneLineReviews);
    }
}
