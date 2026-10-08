package com.pickeat.pickeatbackend.domain.restaurant.service;

import com.pickeat.pickeatbackend.domain.restaurant.dto.RestaurantReportRequest;
import com.pickeat.pickeatbackend.domain.restaurant.dto.RestaurantReportResponse;
import com.pickeat.pickeatbackend.domain.restaurant.entity.RestaurantReport;
import com.pickeat.pickeatbackend.domain.restaurant.entity.RestaurantReportStatus;
import com.pickeat.pickeatbackend.domain.restaurant.exception.RestaurantErrorCode;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantReportRepository;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestaurantReportService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantReportRepository restaurantReportRepository;

    // 신고는 바로 반영하지 않고 PENDING으로 쌓는다. 같은 식당을 검토 전에 또 신고하면 409.
    @Transactional
    public RestaurantReportResponse report(Long restaurantId, RestaurantReportRequest request, Long memberId) {
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND);
        }
        if (restaurantReportRepository.existsByMemberIdAndRestaurantIdAndStatus(
                memberId, restaurantId, RestaurantReportStatus.PENDING)) {
            throw new BusinessException(RestaurantErrorCode.REPORT_ALREADY_PENDING);
        }
        RestaurantReport report = RestaurantReport.builder()
                .memberId(memberId)
                .restaurantId(restaurantId)
                .reason(request.reason())
                .detail(normalize(request.detail()))
                .build();
        try {
            return RestaurantReportResponse.from(restaurantReportRepository.saveAndFlush(report));
        } catch (DataIntegrityViolationException e) {
            // 동시에 두 번 눌러 부분 유니크 인덱스에 걸린 경우
            throw new BusinessException(RestaurantErrorCode.REPORT_ALREADY_PENDING);
        }
    }

    private static String normalize(String detail) {
        if (detail == null || detail.isBlank()) {
            return null;
        }
        return detail.strip();
    }
}
