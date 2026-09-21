package com.pickeat.pickeatbackend.domain.restaurant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlaceResponse;
import com.pickeat.pickeatbackend.domain.restaurant.dto.RestaurantNavigationLinksResponse;
import com.pickeat.pickeatbackend.domain.restaurant.dto.RestaurantResponse;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private RestaurantService restaurantService;

    @Test
    @DisplayName("존재하지 않는 식당이면 예외가 발생한다")
    void throwsWhenRestaurantNotFound() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.getRestaurant(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("존재하지 않는 식당의 지도 링크를 조회하면 예외가 발생한다")
    void throwsWhenNavigationLinksRestaurantNotFound() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.getNavigationLinks(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("지도 링크 조회에 성공한다")
    void returnsNavigationLinks() {
        Restaurant restaurant = Restaurant.builder()
                .name("테스트 식당")
                .latitude(37.58)
                .longitude(127.0)
                .build();
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));

        RestaurantNavigationLinksResponse response = restaurantService.getNavigationLinks(1L);

        assertThat(response.kakaoMapUrl()).contains("37.58,127.0");
    }

    @Test
    @DisplayName("식당 상세 조회에 성공한다")
    void returnsRestaurantDetail() {
        Restaurant restaurant = Restaurant.builder()
                .name("테스트 식당")
                .foodCategory(FoodCategory.KOREAN)
                .latitude(37.58)
                .longitude(127.0)
                .build();
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));

        RestaurantResponse response = restaurantService.getRestaurant(1L);

        assertThat(response.name()).isEqualTo("테스트 식당");
        assertThat(response.foodCategory()).isEqualTo(FoodCategory.KOREAN);
    }

    private GooglePlaceResponse place(String primaryType, Double rating) {
        return new GooglePlaceResponse(
                "place-1",
                new GooglePlaceResponse.DisplayName("맛있는 식당", "ko"),
                "서울시 강남구",
                new GooglePlaceResponse.Location(37.5, 127.0),
                rating,
                42,
                "https://maps.google.com/place-1",
                List.of(),
                primaryType
        );
    }

    @Test
    @DisplayName("신규 식당이면 새로 생성해서 저장한다")
    void createsNewRestaurantWhenNotExisting() {
        when(restaurantRepository.findByGooglePlaceId("place-1")).thenReturn(Optional.empty());
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Restaurant saved = restaurantService.upsertFromGoogle(place("korean_restaurant", 4.5));

        assertThat(saved.getGooglePlaceId()).isEqualTo("place-1");
        assertThat(saved.getName()).isEqualTo("맛있는 식당");
        assertThat(saved.getFoodCategory()).isEqualTo(FoodCategory.KOREAN);
        assertThat(saved.getExternalRating()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
    }

    @Test
    @DisplayName("기존 식당이면 큐레이션 필드는 유지한 채 외부 필드만 갱신한다")
    void updatesExternalFieldsButKeepsCurationFieldsForExistingRestaurant() {
        Restaurant existing = Restaurant.builder()
                .googlePlaceId("place-1")
                .name("옛날 이름")
                .foodCategory(FoodCategory.CHINESE)
                .latitude(37.0)
                .longitude(127.0)
                .suitableForDate(true)
                .build();
        when(restaurantRepository.findByGooglePlaceId("place-1")).thenReturn(Optional.of(existing));
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Restaurant updated = restaurantService.upsertFromGoogle(place("japanese_restaurant", 3.8));

        assertThat(updated.getName()).isEqualTo("맛있는 식당");
        assertThat(updated.getFoodCategory()).isEqualTo(FoodCategory.JAPANESE);
        assertThat(updated.getSuitableForDate()).isTrue();
    }

    @Test
    @DisplayName("유형이나 평점이 없는 부분 데이터도 기존 분류를 유지한 채 갱신된다")
    void keepsExistingFoodCategoryWhenGoogleDataIsPartial() {
        Restaurant existing = Restaurant.builder()
                .googlePlaceId("place-1")
                .name("옛날 이름")
                .foodCategory(FoodCategory.CHINESE)
                .latitude(37.0)
                .longitude(127.0)
                .externalRating(BigDecimal.valueOf(4.5))
                .build();
        when(restaurantRepository.findByGooglePlaceId("place-1")).thenReturn(Optional.of(existing));
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Restaurant updated = restaurantService.upsertFromGoogle(place(null, null));

        assertThat(updated.getFoodCategory()).isEqualTo(FoodCategory.CHINESE);
        assertThat(updated.getExternalRating()).isNull();
    }
}
