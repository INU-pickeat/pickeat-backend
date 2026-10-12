package com.pickeat.pickeatbackend.domain.restaurant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlaceResponse;
import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlacesClient;
import com.pickeat.pickeatbackend.domain.restaurant.entity.DataProvider;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class RestaurantContactBackfillJobTest {

    @Mock RestaurantRepository restaurantRepository;
    @Mock GooglePlacesClient googlePlacesClient;

    @Test
    @DisplayName("월 한도만큼 조회해 연락처를 채우고, 실패한 식당은 다음 달에 다시 시도하도록 남긴다")
    void fillsContactsWithinMonthlyLimitAndLeavesFailuresForRetry() {
        Restaurant ok = restaurant("place-ok");
        Restaurant broken = restaurant("place-broken");
        when(restaurantRepository.findByDataProviderAndContactFetchedAtIsNullOrderByIdAsc(
                DataProvider.GOOGLE, Limit.of(2))).thenReturn(List.of(ok, broken));
        when(googlePlacesClient.findContact("place-ok")).thenReturn(new GooglePlaceResponse(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                new GooglePlaceResponse.OpeningHours(List.of(), List.of("월요일: 휴무일")), "02-123-4567"));
        when(googlePlacesClient.findContact("place-broken")).thenThrow(new RestClientException("timeout"));

        new RestaurantContactBackfillJob(restaurantRepository, googlePlacesClient, 2).run();

        assertThat(ok.getPhoneNumber()).isEqualTo("02-123-4567");
        assertThat(ok.getOpeningHoursText()).isEqualTo("월요일: 휴무일");
        assertThat(ok.getContactFetchedAt()).isNotNull();
        verify(restaurantRepository).save(ok);
        assertThat(broken.getContactFetchedAt()).isNull();
        verify(restaurantRepository, never()).save(broken);
    }

    private Restaurant restaurant(String placeId) {
        return Restaurant.builder()
                .googlePlaceId(placeId)
                .name("맛있는 식당")
                .latitude(37.5)
                .longitude(127.0)
                .build();
    }
}
