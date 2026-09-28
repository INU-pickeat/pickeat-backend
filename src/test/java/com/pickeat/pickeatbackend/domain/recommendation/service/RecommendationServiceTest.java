package com.pickeat.pickeatbackend.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pickeat.pickeatbackend.domain.member.entity.Member;
import com.pickeat.pickeatbackend.domain.member.repository.MemberRepository;
import com.pickeat.pickeatbackend.domain.recommendation.dto.RecommendationExclusionRequest;
import com.pickeat.pickeatbackend.domain.recommendation.dto.RecommendationRequest;
import com.pickeat.pickeatbackend.domain.recommendation.dto.RecommendationRequest.PriceRange;
import com.pickeat.pickeatbackend.domain.recommendation.dto.RecommendationResponse;
import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.recommendation.entity.ExclusionReason;
import com.pickeat.pickeatbackend.domain.recommendation.entity.RecommendationCandidate;
import com.pickeat.pickeatbackend.domain.recommendation.entity.RecommendationExclusion;
import com.pickeat.pickeatbackend.domain.recommendation.entity.RecommendationSession;
import com.pickeat.pickeatbackend.domain.recommendation.repository.RecommendationCandidateRepository;
import com.pickeat.pickeatbackend.domain.recommendation.repository.RecommendationExclusionRepository;
import com.pickeat.pickeatbackend.domain.recommendation.repository.RecommendationSessionRepository;
import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlaceResponse;
import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlacesClient;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantCandidate;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import com.pickeat.pickeatbackend.domain.restaurant.service.RestaurantService;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
    private RecommendationExclusionRepository exclusionRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private Member member;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(
                googlePlacesClient, restaurantService, restaurantRepository, new RecommendationScoreCalculator(),
                sessionRepository, candidateRepository, exclusionRepository, memberRepository);
    }

    private RecommendationRequest request(CompanionType companionType) {
        return new RecommendationRequest(Set.of(FoodCategory.KOREAN), companionType, null, 37.5, 127.0);
    }

    private RecommendationRequest requestWithPriceRange(BigDecimal min, BigDecimal max) {
        return new RecommendationRequest(
                Set.of(FoodCategory.KOREAN), CompanionType.DATE, new PriceRange(min, max), 37.5, 127.0);
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

    private Restaurant restaurantWithPrice(BigDecimal priceRangeStart, BigDecimal priceRangeEnd) {
        return Restaurant.builder()
                .name("식당")
                .foodCategory(FoodCategory.KOREAN)
                .latitude(37.5)
                .longitude(127.0)
                .externalRating(BigDecimal.valueOf(4.0))
                .priceRangeStart(priceRangeStart)
                .priceRangeEnd(priceRangeEnd)
                .build();
    }

    private Restaurant restaurantWithId(Long id, String name) {
        Restaurant restaurant = Restaurant.builder()
                .name(name)
                .foodCategory(FoodCategory.KOREAN)
                .latitude(37.5)
                .longitude(127.0)
                .externalRating(BigDecimal.valueOf(4.0))
                .build();
        ReflectionTestUtils.setField(restaurant, "id", id);
        return restaurant;
    }

    private RecommendationCandidate candidate(Restaurant restaurant, int resultRank) {
        return RecommendationCandidate.builder()
                .restaurant(restaurant)
                .resultRank(resultRank)
                .distanceMeters(100.0)
                .ratingContribution(0.5)
                .distanceContribution(0.3)
                .companionBonus(0.0)
                .totalScore(0.8)
                .build();
    }

    private RecommendationExclusion exclusionOf(Restaurant restaurant) {
        return RecommendationExclusion.builder()
                .restaurant(restaurant)
                .reason(ExclusionReason.WANT_DIFFERENT)
                .build();
    }

    private RecommendationSession sessionWithId(Long id) {
        RecommendationSession session = RecommendationSession.builder()
                .member(member).companionType(CompanionType.DATE).latitude(37.5).longitude(127.0)
                .foodCategories(Set.of(FoodCategory.KOREAN)).build();
        ReflectionTestUtils.setField(session, "id", id);
        return session;
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
        assertThat(response.items().get(4).score()).isGreaterThanOrEqualTo(0.24);
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
                Set.of(FoodCategory.OTHER), CompanionType.DATE, null, 37.5, 127.0);
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

    @Test
    @DisplayName("제외 대상 세션이 없으면 예외가 발생한다")
    void throwsWhenExcludingFromMissingSession() {
        when(sessionRepository.findByIdAndMemberId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recommendationService.exclude(
                10L, 1L, new RecommendationExclusionRequest(100L, ExclusionReason.WANT_DIFFERENT)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("세션 후보에 없는 식당을 제외하려 하면 예외가 발생한다")
    void throwsWhenExcludingRestaurantNotInSessionCandidates() {
        when(sessionRepository.findByIdAndMemberId(10L, 1L)).thenReturn(Optional.of(sessionWithId(10L)));
        when(candidateRepository.existsBySessionIdAndRestaurantId(10L, 100L)).thenReturn(false);

        assertThatThrownBy(() -> recommendationService.exclude(
                10L, 1L, new RecommendationExclusionRequest(100L, ExclusionReason.WANT_DIFFERENT)))
                .isInstanceOf(BusinessException.class);

        verify(exclusionRepository, never()).save(any());
    }

    @Test
    @DisplayName("제외한 식당은 다음 순위 후보로 대체되어 노출된다")
    void promotesNextCandidateWhenRestaurantExcluded() {
        RecommendationSession session = sessionWithId(10L);
        when(sessionRepository.findByIdAndMemberId(10L, 1L)).thenReturn(Optional.of(session));
        when(candidateRepository.existsBySessionIdAndRestaurantId(10L, 1L)).thenReturn(true);
        when(exclusionRepository.existsBySessionIdAndRestaurantId(10L, 1L)).thenReturn(false);

        List<RecommendationCandidate> stored = IntStream.rangeClosed(1, 6)
                .mapToObj(i -> candidate(restaurantWithId((long) i, "식당" + i), i))
                .toList();
        when(candidateRepository.findBySessionIdOrderByResultRankAsc(10L)).thenReturn(stored);
        // save() 이후 다시 조회했을 때 방금 제외한 식당이 보이는 상태를 흉내낸다(Mock이라 save가 저장소 상태를 바꾸지 않는다).
        when(exclusionRepository.findBySessionId(10L))
                .thenReturn(List.of(exclusionOf(stored.get(0).getRestaurant())));

        RecommendationResponse response = recommendationService.exclude(
                10L, 1L, new RecommendationExclusionRequest(1L, ExclusionReason.DISTANCE_TOO_FAR));

        verify(exclusionRepository).save(any(RecommendationExclusion.class));
        assertThat(response.items()).hasSize(5);
        assertThat(response.items().get(0).restaurantId()).isEqualTo(2L);
        assertThat(response.items().get(0).rank()).isEqualTo(1);
        assertThat(response.items()).extracting(RecommendationResponse.Item::restaurantId)
                .doesNotContain(1L);
    }

    @Test
    @DisplayName("이미 제외한 식당을 다시 제외해도 중복 저장하지 않는다")
    void isIdempotentWhenRestaurantAlreadyExcluded() {
        RecommendationSession session = sessionWithId(10L);
        when(sessionRepository.findByIdAndMemberId(10L, 1L)).thenReturn(Optional.of(session));
        when(candidateRepository.existsBySessionIdAndRestaurantId(10L, 1L)).thenReturn(true);
        when(exclusionRepository.existsBySessionIdAndRestaurantId(10L, 1L)).thenReturn(true);
        when(candidateRepository.findBySessionIdOrderByResultRankAsc(10L)).thenReturn(List.of());
        when(exclusionRepository.findBySessionId(10L)).thenReturn(List.of());

        recommendationService.exclude(10L, 1L, new RecommendationExclusionRequest(1L, ExclusionReason.WANT_DIFFERENT));

        verify(exclusionRepository, never()).save(any());
    }

    @Test
    @DisplayName("대체 후보가 소진되면 5개보다 적게 노출한다")
    void returnsFewerThanFiveWhenNoAlternatesRemain() {
        RecommendationSession session = sessionWithId(10L);
        when(sessionRepository.findByIdAndMemberId(10L, 1L)).thenReturn(Optional.of(session));
        when(candidateRepository.existsBySessionIdAndRestaurantId(10L, 1L)).thenReturn(true);
        when(exclusionRepository.existsBySessionIdAndRestaurantId(10L, 1L)).thenReturn(false);

        List<RecommendationCandidate> stored = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> candidate(restaurantWithId((long) i, "식당" + i), i))
                .toList();
        when(candidateRepository.findBySessionIdOrderByResultRankAsc(10L)).thenReturn(stored);
        when(exclusionRepository.findBySessionId(10L))
                .thenReturn(List.of(exclusionOf(stored.get(0).getRestaurant())));

        RecommendationResponse response = recommendationService.exclude(
                10L, 1L, new RecommendationExclusionRequest(1L, ExclusionReason.PRICE_TOO_HIGH));

        assertThat(response.items()).hasSize(4);
    }

    @Test
    @DisplayName("DB 유효 후보가 10개 이상이면 Google을 호출하지 않는다")
    void skipsGoogleWhenTenOrMoreValidDbCandidatesExist() {
        stubPersistence();
        List<RestaurantCandidate> candidates = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> new RestaurantCandidate(restaurant(FoodCategory.KOREAN, i % 5 + 1.0, null), i * 100))
                .toList();
        when(restaurantRepository.findWithinRadius(37.5, 127.0, 5000.0)).thenReturn(candidates);

        RecommendationResponse response = recommendationService.recommend(request(CompanionType.DATE), 1L);

        verify(googlePlacesClient, never()).findNearbyRestaurants(
                anyDouble(), anyDouble(), any(GooglePlacesClient.RankPreference.class), anySet());
        assertThat(response.items()).hasSize(5);
    }

    @Test
    @DisplayName("DB 유효 후보가 10개 미만이면 Google을 호출해 보충한 뒤 DB를 다시 조회한다")
    void callsGoogleAndRequeriesWhenFewerThanTenValidDbCandidatesExist() {
        stubPersistence();
        GooglePlaceResponse place = new GooglePlaceResponse(
                "place-1", new GooglePlaceResponse.DisplayName("맛집", "ko"), "주소",
                new GooglePlaceResponse.Location(37.5, 127.0), 4.5, 10, "uri", List.of(), "korean_restaurant");
        when(googlePlacesClient.findNearbyRestaurants(
                eq(37.5), eq(127.0), eq(GooglePlacesClient.RankPreference.POPULARITY), anySet()))
                .thenReturn(List.of(place));
        List<RestaurantCandidate> initial = IntStream.rangeClosed(1, 3)
                .mapToObj(i -> new RestaurantCandidate(restaurant(FoodCategory.KOREAN, i, null), i * 100))
                .toList();
        when(restaurantRepository.findWithinRadius(37.5, 127.0, 5000.0)).thenReturn(initial);

        recommendationService.recommend(request(CompanionType.DATE), 1L);

        verify(restaurantService).upsertFromGoogle(place);
        verify(restaurantRepository, times(2)).findWithinRadius(37.5, 127.0, 5000.0);
    }

    @Test
    @DisplayName("유효 후보가 10개보다 많으면 상위 10개까지 저장하고 응답은 상위 5개만 반환한다")
    void storesUpToTenCandidatesButReturnsOnlyTopFive() {
        stubPersistence();
        List<RestaurantCandidate> candidates = IntStream.rangeClosed(1, 12)
                .mapToObj(i -> new RestaurantCandidate(restaurant(FoodCategory.KOREAN, i % 5 + 0.5, null), i * 100))
                .toList();
        when(restaurantRepository.findWithinRadius(37.5, 127.0, 5000.0)).thenReturn(candidates);
        ArgumentCaptor<List<RecommendationCandidate>> captor = ArgumentCaptor.forClass(List.class);

        RecommendationResponse response = recommendationService.recommend(request(CompanionType.DATE), 1L);

        verify(candidateRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(10);
        assertThat(response.items()).hasSize(5);
    }

    @Test
    @DisplayName("가격대를 생략하면 가격 정보가 없는 식당도 포함된다")
    void includesRestaurantsWithoutPriceInfoWhenPriceRangeOmitted() {
        stubPersistence();
        stubEmptyGoogleSearch();
        RestaurantCandidate noPriceInfo = new RestaurantCandidate(restaurantWithPrice(null, null), 100);
        when(restaurantRepository.findWithinRadius(37.5, 127.0, 5000.0)).thenReturn(List.of(noPriceInfo));

        RecommendationResponse response = recommendationService.recommend(request(CompanionType.DATE), 1L);

        assertThat(response.items()).hasSize(1);
    }

    @Test
    @DisplayName("가격대를 지정하면 가격 정보가 없는 식당은 제외된다")
    void excludesRestaurantsWithoutPriceInfoWhenPriceRangeSpecified() {
        stubPersistence();
        stubEmptyGoogleSearch();
        RestaurantCandidate noPriceInfo = new RestaurantCandidate(restaurantWithPrice(null, null), 100);
        when(restaurantRepository.findWithinRadius(37.5, 127.0, 5000.0)).thenReturn(List.of(noPriceInfo));

        RecommendationResponse response = recommendationService.recommend(
                requestWithPriceRange(BigDecimal.valueOf(10000), BigDecimal.valueOf(30000)), 1L);

        assertThat(response.items()).isEmpty();
    }

    @Test
    @DisplayName("요청 가격대와 식당 가격대가 겹치면 포함된다")
    void includesRestaurantsWithOverlappingPriceRange() {
        stubPersistence();
        stubEmptyGoogleSearch();
        RestaurantCandidate overlapping = new RestaurantCandidate(
                restaurantWithPrice(BigDecimal.valueOf(20000), BigDecimal.valueOf(50000)), 100);
        when(restaurantRepository.findWithinRadius(37.5, 127.0, 5000.0)).thenReturn(List.of(overlapping));

        RecommendationResponse response = recommendationService.recommend(
                requestWithPriceRange(BigDecimal.valueOf(10000), BigDecimal.valueOf(30000)), 1L);

        assertThat(response.items()).hasSize(1);
    }

    @Test
    @DisplayName("요청 가격대와 식당 가격대가 겹치지 않으면 제외된다")
    void excludesRestaurantsWithNonOverlappingPriceRange() {
        stubPersistence();
        stubEmptyGoogleSearch();
        RestaurantCandidate tooExpensive = new RestaurantCandidate(
                restaurantWithPrice(BigDecimal.valueOf(50000), BigDecimal.valueOf(80000)), 100);
        when(restaurantRepository.findWithinRadius(37.5, 127.0, 5000.0)).thenReturn(List.of(tooExpensive));

        RecommendationResponse response = recommendationService.recommend(
                requestWithPriceRange(BigDecimal.valueOf(10000), BigDecimal.valueOf(30000)), 1L);

        assertThat(response.items()).isEmpty();
    }

    @Test
    @DisplayName("세션에 요청한 가격대를 저장한다")
    void savesPriceRangeOnSession() {
        stubPersistence();
        stubEmptyGoogleSearch();
        when(restaurantRepository.findWithinRadius(37.5, 127.0, 5000.0)).thenReturn(List.of());
        ArgumentCaptor<RecommendationSession> captor = ArgumentCaptor.forClass(RecommendationSession.class);

        recommendationService.recommend(
                requestWithPriceRange(BigDecimal.valueOf(10000), BigDecimal.valueOf(30000)), 1L);

        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getPriceRangeMin()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(captor.getValue().getPriceRangeMax()).isEqualByComparingTo(BigDecimal.valueOf(30000));
    }
}
