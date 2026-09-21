package com.pickeat.pickeatbackend.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pickeat.pickeatbackend.domain.member.entity.Member;
import com.pickeat.pickeatbackend.domain.member.repository.MemberRepository;
import com.pickeat.pickeatbackend.domain.recommendation.dto.RecommendationRequest;
import com.pickeat.pickeatbackend.domain.recommendation.dto.RecommendationResponse;
import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.recommendation.entity.RecommendationSession;
import com.pickeat.pickeatbackend.domain.recommendation.repository.RecommendationCandidateRepository;
import com.pickeat.pickeatbackend.domain.recommendation.repository.RecommendationSessionRepository;
import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlaceResponse;
import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlacesClient;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantCandidate;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import com.pickeat.pickeatbackend.domain.restaurant.service.RestaurantService;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private GooglePlacesClient googlePlacesClient;
    @Mock
    private RestaurantService restaurantService;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private RecommendationSessionRepository sessionRepository;
    @Mock
    private RecommendationCandidateRepository candidateRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private Member member;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(
                googlePlacesClient, restaurantService, restaurantRepository,
                new RecommendationScoreCalculator(), sessionRepository, candidateRepository, memberRepository);
    }

    private RecommendationRequest request(CompanionType companionType) {
        return new RecommendationRequest(Set.of(FoodCategory.KOREAN), companionType, 37.5, 127.0);
    }

    private Restaurant restaurant(FoodCategory foodCategory, double rating, Boolean suitableForDate) {
        return Restaurant.builder()
                .name("식당")
                .foodCategory(foodCategory)
                .latitude(37.5)
                .longitude(127.0)
                .externalRating(java.math.BigDecimal.valueOf(rating))
                .suitableForDate(suitableForDate)
                .build();
    }

    private void stubPersistence() {
        when(memberRepository.getReferenceById(1L)).thenReturn(member);
        when(sessionRepository.save(any(RecommendationSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(candidateRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubEmptyGoogleSearch() {
        when(googlePlacesClient.findNearbyRestaurants(
                eq(37.5), eq(127.0), eq(GooglePlacesClient.RankPreference.POPULARITY), anySet()))
                .thenReturn(List.of());
    }

    @Test
    void 요청_카테고리와_다른_식당은_후보에서_제외된다() {
        stubPersistence();
        stubEmptyGoogleSearch();
        RestaurantCandidate matching = new RestaurantCandidate(restaurant(FoodCategory.KOREAN, 4.0, null), 1000);
        RestaurantCandidate notMatching = new RestaurantCandidate(restaurant(FoodCategory.JAPANESE, 5.0, null), 100);
        when(restaurantRepository.findWithinRadius(37.5, 127.0, 5000.0)).thenReturn(List.of(matching, notMatching));

        RecommendationResponse response = recommendationService.recommend(request(CompanionType.DATE), 1L);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).foodCategory()).isEqualTo(FoodCategory.KOREAN);
    }

    @Test
    void 점수_내림차순으로_정렬되고_순위가_매겨진다() {
        stubPersistence();
        stubEmptyGoogleSearch();
        RestaurantCandidate closeHighRated = new RestaurantCandidate(restaurant(FoodCategory.KOREAN, 5.0, null), 0);
        RestaurantCandidate farLowRated = new RestaurantCandidate(restaurant(FoodCategory.KOREAN, 1.0, null), 4900);
        when(restaurantRepository.findWithinRadius(37.5, 127.0, 5000.0))
                .thenReturn(List.of(farLowRated, closeHighRated));

        RecommendationResponse response = recommendationService.recommend(request(CompanionType.DATE), 1L);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).rank()).isEqualTo(1);
        assertThat(response.items().get(0).distanceMeters()).isEqualTo(0);
        assertThat(response.items().get(1).rank()).isEqualTo(2);
        assertThat(response.items().get(0).score()).isGreaterThan(response.items().get(1).score());
    }

    @Test
    void 동행_적합_식당이_불일치_식당보다_높은_점수를_받는다() {
        stubPersistence();
        stubEmptyGoogleSearch();
        RestaurantCandidate suitable = new RestaurantCandidate(restaurant(FoodCategory.KOREAN, 3.0, true), 1000);
        RestaurantCandidate notSuitable = new RestaurantCandidate(restaurant(FoodCategory.KOREAN, 3.0, false), 1000);
        when(restaurantRepository.findWithinRadius(37.5, 127.0, 5000.0))
                .thenReturn(List.of(notSuitable, suitable));

        RecommendationResponse response = recommendationService.recommend(request(CompanionType.DATE), 1L);

        assertThat(response.items().get(0).rank()).isEqualTo(1);
        assertThat(response.items().get(0).score()).isGreaterThan(response.items().get(1).score());
    }

    @Test
    void Google_검색_결과는_모두_upsert된다() {
        stubPersistence();
        GooglePlaceResponse place = new GooglePlaceResponse(
                "place-1", new GooglePlaceResponse.DisplayName("맛집", "ko"), "주소",
                new GooglePlaceResponse.Location(37.5, 127.0), 4.5, 10, "uri", List.of(), "korean_restaurant");
        when(googlePlacesClient.findNearbyRestaurants(
                eq(37.5), eq(127.0), eq(GooglePlacesClient.RankPreference.POPULARITY), anySet()))
                .thenReturn(List.of(place));
        when(restaurantRepository.findWithinRadius(37.5, 127.0, 5000.0)).thenReturn(List.of());

        recommendationService.recommend(request(CompanionType.DATE), 1L);

        verify(restaurantService).upsertFromGoogle(place);
    }

    @Test
    void 세션_주인이_아니면_예외가_발생한다() {
        when(member.getId()).thenReturn(2L);
        RecommendationSession session = RecommendationSession.builder()
                .member(member).companionType(CompanionType.DATE).latitude(37.5).longitude(127.0)
                .foodCategories(Set.of(FoodCategory.KOREAN)).build();
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> recommendationService.getSession(10L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 존재하지_않는_세션이면_예외가_발생한다() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recommendationService.getSession(10L, 1L))
                .isInstanceOf(BusinessException.class);
    }
}
