package com.pickeat.pickeatbackend.domain.restaurant.service;

import com.pickeat.pickeatbackend.domain.restaurant.dto.RestaurantResponse;
import com.pickeat.pickeatbackend.domain.restaurant.exception.RestaurantErrorCode;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurant(Long restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .map(RestaurantResponse::from)
                .orElseThrow(() -> new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND));
    }
}
