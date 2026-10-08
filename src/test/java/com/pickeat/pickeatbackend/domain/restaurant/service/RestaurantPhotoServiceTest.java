package com.pickeat.pickeatbackend.domain.restaurant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlacesClient;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.domain.restaurant.exception.RestaurantErrorCode;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import com.pickeat.pickeatbackend.global.exception.GlobalErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

@ExtendWith(MockitoExtension.class)
class RestaurantPhotoServiceTest {

    private static final String PHOTO_URI = "https://lh3.googleusercontent.com/test-photo";

    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private GooglePlacesClient googlePlacesClient;

    @InjectMocks
    private RestaurantPhotoService restaurantPhotoService;

    private void stubRestaurant() {
        Restaurant restaurant = Restaurant.builder()
                .name("테스트 식당").googlePlaceId("place-1").latitude(37.5).longitude(127.0).build();
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
    }

    @Test
    @DisplayName("Google에서 받은 사진 주소를 돌려준다")
    void returnsPhotoUriFromGoogle() {
        stubRestaurant();
        when(googlePlacesClient.findPhotoUri("place-1", 0, RestaurantPhotoService.MAX_WIDTH_PX))
                .thenReturn(Optional.of(PHOTO_URI));

        assertThat(restaurantPhotoService.getPhotoUri(1L)).isEqualTo(PHOTO_URI);
    }

    @Test
    @DisplayName("같은 식당을 다시 요청하면 Google을 다시 부르지 않는다")
    void reusesCachedPhotoUri() {
        stubRestaurant();
        when(googlePlacesClient.findPhotoUri("place-1", 0, RestaurantPhotoService.MAX_WIDTH_PX))
                .thenReturn(Optional.of(PHOTO_URI));

        restaurantPhotoService.getPhotoUri(1L);
        restaurantPhotoService.getPhotoUri(1L);

        verify(googlePlacesClient, times(1)).findPhotoUri("place-1", 0, RestaurantPhotoService.MAX_WIDTH_PX);
    }

    @Test
    @DisplayName("사진이 없는 식당은 PHOTO_NOT_FOUND이고 그 결과도 기억한다")
    void throwsAndCachesWhenRestaurantHasNoPhoto() {
        stubRestaurant();
        when(googlePlacesClient.findPhotoUri("place-1", 0, RestaurantPhotoService.MAX_WIDTH_PX))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantPhotoService.getPhotoUri(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RestaurantErrorCode.PHOTO_NOT_FOUND));
        assertThatThrownBy(() -> restaurantPhotoService.getPhotoUri(1L)).isInstanceOf(BusinessException.class);

        verify(googlePlacesClient, times(1)).findPhotoUri("place-1", 0, RestaurantPhotoService.MAX_WIDTH_PX);
    }

    @Test
    @DisplayName("Google 통신 오류는 PHOTO_NOT_FOUND로 응답하되 기억하지 않는다")
    void doesNotCacheGoogleFailure() {
        stubRestaurant();
        when(googlePlacesClient.findPhotoUri("place-1", 0, RestaurantPhotoService.MAX_WIDTH_PX))
                .thenThrow(new ResourceAccessException("timeout"))
                .thenReturn(Optional.of(PHOTO_URI));

        assertThatThrownBy(() -> restaurantPhotoService.getPhotoUri(1L)).isInstanceOf(BusinessException.class);

        assertThat(restaurantPhotoService.getPhotoUri(1L)).isEqualTo(PHOTO_URI);
    }

    @Test
    @DisplayName("없는 식당이면 RESTAURANT_NOT_FOUND")
    void throwsWhenRestaurantNotFound() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantPhotoService.getPhotoUri(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(RestaurantErrorCode.RESTAURANT_NOT_FOUND));
    }

    @Test
    @DisplayName("사진 순번마다 따로 조회하고 따로 기억한다")
    void fetchesAndCachesEachPhotoIndexSeparately() {
        stubRestaurant();
        when(googlePlacesClient.findPhotoUri("place-1", 0, RestaurantPhotoService.MAX_WIDTH_PX))
                .thenReturn(Optional.of(PHOTO_URI));
        when(googlePlacesClient.findPhotoUri("place-1", 2, RestaurantPhotoService.MAX_WIDTH_PX))
                .thenReturn(Optional.of(PHOTO_URI + "-third"));

        assertThat(restaurantPhotoService.getPhotoUri(1L, 0)).isEqualTo(PHOTO_URI);
        assertThat(restaurantPhotoService.getPhotoUri(1L, 2)).isEqualTo(PHOTO_URI + "-third");
        assertThat(restaurantPhotoService.getPhotoUri(1L, 2)).isEqualTo(PHOTO_URI + "-third");

        verify(googlePlacesClient, times(1)).findPhotoUri("place-1", 2, RestaurantPhotoService.MAX_WIDTH_PX);
    }

    @Test
    @DisplayName("사진 순번이 0~2 밖이면 Google을 부르지 않고 입력 오류로 응답한다")
    void rejectsPhotoIndexOutOfRange() {
        assertThatThrownBy(() -> restaurantPhotoService.getPhotoUri(1L, 3))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(GlobalErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> restaurantPhotoService.getPhotoUri(1L, -1)).isInstanceOf(BusinessException.class);
    }
}
