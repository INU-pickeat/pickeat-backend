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
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("요청 카테고리와 다른 식당은 후보에서 제외된다")
    void excludesRestaurantsNotMatchingRequestedCategory() {
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
    @DisplayName("점수 내림차순으로 정렬되고 순위가 매겨진다")
    void ranksCandidatesByScoreDescending() {
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
    @DisplayName("조건을 만족하는 후보가 없으면 빈 목록을 반환한다")
    void returnsEmptyItemsWhenNoCandidateMatches() {
        stubPersistence();
        stubEmptyGoogleSearch();
        when(restaurantRepository.findWithinRadius(37.5, 127.0, 5000.0)).thenReturn(List.of());

        RecommendationResponse response = recommendationService.recommend(request(CompanionType.DATE), 1L);

        assertThat(response.items()).isEmpty();
    }

    @Test
    @DisplayName("후보가 5개보다 적으면 조회된 후보만 반환한다")
    void returnsAllCandidatesWhenFewerThanFiveExist() {
        stubPersistence();
        stubEmptyGoogleSearch();
        List<RestaurantCandidate> candidates = IntStream.rangeClosed(1, 4)
                .mapToObj(i -> new RestaurantCandidate(restaurant(FoodCategory.KOREAN, i, null), i * 100))
                .toList();
        when(restaurantRepository.findWithinRadius(37.5, 127.0, 5000.0)).thenReturn(candidates);

        RecommendationResponse response = recommendationService.recommend(request(CompanionType.DATE), 1L);

        assertThat(response.items()).hasSize(4);
        assertThat(response.items()).extracting(RecommendationResponse.Item::rank)
                .containsExactly(1, 2, 3, 4);
    }

    @Test
    @DisplayName("후보가 5개보다 많으면 점수가 높은 상위 5개만 반환한다")
    void returnsOnlyTopFiveWhenMoreThanFiveCandidatesExist() {
        stubPersistence();
        stubEmptyGoogleSearch();
        List<RestaurantCandidate> candidates = IntStream.rangeClosed(0, 5)
                .mapToObj(i -> new RestaurantCandidate(restaurant(FoodCategory.KOREAN, i, null), 1000))
                .toList();
        when(restaurantRepository.findWithinRadius(37.5, 127.0, 5000.0)).thenReturn(candidates);

        RecommendationResponse response = recommendationService.recommend(request(CompanionType.DATE), 1L);

        assertThat(response.items()).hasSize(5);
        assertThat(response.items()).extracting(RecommendationResponse.Item::rank)
                .containsExactly(1, 2, 3, 4, 5);
        assertThat(response.items()).extracting(RecommendationResponse.Item::score)
                .isSortedAccordingTo(java.util.Comparator.reverseOrder());
        assertThat(response.items().get(4).score()).isGreaterThan(0.32);
    }

    @Test
    @DisplayName("동행 적합 식당이 불일치 식당보다 높은 점수를 받는다")
    void scoresCompanionMatchHigherThanMismatch() {
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
    @DisplayName("Google 검색 결과는 모두 upsert된다")
    void upsertsAllGoogleSearchResults() {
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
    @DisplayName("Google 주 유형이 50개를 넘으면 여러 요청으로 나누고 장소 ID 중복을 제거한다")
    void batchesGooglePrimaryTypesAndDeduplicatesPlaces() {
        stubPersistence();
        GooglePlaceResponse duplicate = new GooglePlaceResponse(
                "same-place", new GooglePlaceResponse.DisplayName("맛집", "ko"), "주소",
                new GooglePlaceResponse.Location(37.5, 127.0), 4.5, 10, "uri", List.of(), "thai_restaurant");
        when(googlePlacesClient.findNearbyRestaurants(
                eq(37.5), eq(127.0), eq(GooglePlacesClient.RankPreference.POPULARITY), anySet()))
                .thenReturn(List.of(duplicate));
        when(restaurantRepository.findWithinRadius(37.5, 127.0, 5000.0)).thenReturn(List.of());

        RecommendationRequest otherRequest = new RecommendationRequest(
                Set.of(FoodCategory.OTHER), CompanionType.DATE, 37.5, 127.0);
        recommendationService.recommend(otherRequest, 1L);

        verify(googlePlacesClient, org.mockito.Mockito.times(2)).findNearbyRestaurants(
                eq(37.5), eq(127.0), eq(GooglePlacesClient.RankPreference.POPULARITY), anySet());
        verify(restaurantService).upsertFromGoogle(duplicate);
    }

    @Test
    @DisplayName("세션 주인이 아니면 예외가 발생한다")
    void throwsWhenSessionOwnerMismatch() {
        when(sessionRepository.findByIdAndMemberId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recommendationService.getSession(10L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("존재하지 않는 세션이면 예외가 발생한다")
    void throwsWhenSessionNotFound() {
        when(sessionRepository.findByIdAndMemberId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recommendationService.getSession(10L, 1L))
                .isInstanceOf(BusinessException.class);
    }
}
