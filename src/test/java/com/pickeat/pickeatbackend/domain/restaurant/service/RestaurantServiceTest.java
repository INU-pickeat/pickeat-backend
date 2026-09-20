package com.pickeat.pickeatbackend.domain.restaurant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlaceResponse;
import com.pickeat.pickeatbackend.domain.restaurant.dto.RestaurantResponse;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
    void 존재하지_않는_식당이면_예외가_발생한다() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.getRestaurant(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 식당_상세_조회에_성공한다() {
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
    void 신규_식당이면_새로_생성해서_저장한다() {
        when(restaurantRepository.findByGooglePlaceId("place-1")).thenReturn(Optional.empty());
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Restaurant saved = restaurantService.upsertFromGoogle(place("korean_restaurant", 4.5));

        assertThat(saved.getGooglePlaceId()).isEqualTo("place-1");
        assertThat(saved.getName()).isEqualTo("맛있는 식당");
        assertThat(saved.getFoodCategory()).isEqualTo(FoodCategory.KOREAN);
        assertThat(saved.getExternalRating()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
    }

    @Test
    void 기존_식당이면_큐레이션_필드는_유지한_채_외부_필드만_갱신한다() {
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
    void 유형이나_평점이_없는_부분_데이터도_기존_분류를_유지한_채_갱신된다() {
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
