package com.pickeat.pickeatbackend.domain.recommendation.service;

import com.pickeat.pickeatbackend.domain.member.repository.MemberRepository;
import com.pickeat.pickeatbackend.domain.recommendation.dto.RecommendationRequest;
import com.pickeat.pickeatbackend.domain.recommendation.dto.RecommendationResponse;
import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.recommendation.entity.RecommendationCandidate;
import com.pickeat.pickeatbackend.domain.recommendation.entity.RecommendationSession;
import com.pickeat.pickeatbackend.domain.recommendation.exception.RecommendationErrorCode;
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
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// DB 우선, 부족할 때만 Google 하이브리드(M2 계약 개편). DB에서 카테고리·가격 필터를 통과한
// 유효 후보가 10개 미만일 때만 Google Places를 호출해 보충한다. 상위 10개를 세션 후보로
// 저장하고(1~5위 노출, 6~10위는 제외 시 대체), 응답에는 상위 5개만 반환한다.
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final double SEARCH_RADIUS_METERS = 5000.0;
    private static final int TOP_RESULT_COUNT = 5;
    private static final int STORED_CANDIDATE_COUNT = 10;
    private static final int MIN_DB_CANDIDATES_BEFORE_GOOGLE = 10;

    private final GooglePlacesClient googlePlacesClient;
    private final RestaurantService restaurantService;
    private final RestaurantRepository restaurantRepository;
    private final RecommendationScoreCalculator scoreCalculator;
    private final RecommendationSessionRepository sessionRepository;
    private final RecommendationCandidateRepository candidateRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public RecommendationResponse recommend(RecommendationRequest request, Long memberId) {
        List<RestaurantCandidate> candidates = findValidCandidates(request);

        if (candidates.size() < MIN_DB_CANDIDATES_BEFORE_GOOGLE) {
            Set<String> includedTypes = FoodCategory.toGooglePrimaryTypes(request.foodCategories());
            findGooglePlaces(request, includedTypes).forEach(restaurantService::upsertFromGoogle);
            candidates = findValidCandidates(request);
        }

        List<ScoredCandidate> topCandidates = candidates.stream()
                .map(candidate -> score(candidate, request.companionType()))
                .sorted(Comparator.comparingDouble((ScoredCandidate c) -> c.breakdown().totalScore()).reversed())
                .limit(STORED_CANDIDATE_COUNT)
                .toList();

        RecommendationRequest.PriceRange priceRange = request.priceRange();
        RecommendationSession session = sessionRepository.save(RecommendationSession.builder()
                .member(memberRepository.getReferenceById(memberId))
                .companionType(request.companionType())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .priceRangeMin(priceRange == null ? null : priceRange.min())
                .priceRangeMax(priceRange == null ? null : priceRange.max())
                .foodCategories(request.foodCategories())
                .build());

        List<RecommendationCandidate> savedCandidates = candidateRepository.saveAll(
                toCandidateEntities(session, topCandidates));

        return RecommendationResponse.of(session.getId(), topRanked(savedCandidates));
    }

    // DB에서 5km 이내 후보를 조회한 뒤 카테고리·가격 조건을 통과한 후보만 남긴다.
    private List<RestaurantCandidate> findValidCandidates(RecommendationRequest request) {
        return restaurantRepository.findWithinRadius(request.latitude(), request.longitude(), SEARCH_RADIUS_METERS)
                .stream()
                .filter(candidate -> request.foodCategories().contains(candidate.restaurant().getFoodCategory()))
                .filter(candidate -> matchesPriceRange(candidate.restaurant(), request.priceRange()))
                .toList();
    }

    // 가격 무관이면 전부 포함, 가격 지정 시 식당에 가격 정보가 없으면 제외, 있으면 범위가
    // 일부라도 겹칠 때 포함한다. 식당 쪽 상한/하한이 없으면(NULL) 그 방향은 무제한으로 본다.
    private boolean matchesPriceRange(Restaurant restaurant, RecommendationRequest.PriceRange priceRange) {
        if (priceRange == null) {
            return true;
        }
        BigDecimal restaurantMin = restaurant.getPriceRangeStart();
        BigDecimal restaurantMax = restaurant.getPriceRangeEnd();
        if (restaurantMin == null && restaurantMax == null) {
            return false;
        }
        boolean withinRequestMax = restaurantMin == null || restaurantMin.compareTo(priceRange.max()) <= 0;
        boolean withinRequestMin = restaurantMax == null || restaurantMax.compareTo(priceRange.min()) >= 0;
        return withinRequestMax && withinRequestMin;
    }

    private List<RecommendationCandidate> topRanked(List<RecommendationCandidate> candidates) {
        return candidates.stream().filter(candidate -> candidate.getResultRank() <= TOP_RESULT_COUNT).toList();
    }

    private List<GooglePlaceResponse> findGooglePlaces(RecommendationRequest request, Set<String> includedTypes) {
        List<String> types = new ArrayList<>(includedTypes);
        Map<String, GooglePlaceResponse> uniquePlaces = new LinkedHashMap<>();
        for (int start = 0; start < types.size(); start += 50) {
            Set<String> batch = Set.copyOf(types.subList(start, Math.min(start + 50, types.size())));
            googlePlacesClient.findNearbyRestaurants(
                            request.latitude(), request.longitude(), GooglePlacesClient.RankPreference.POPULARITY, batch)
                    .forEach(place -> uniquePlaces.putIfAbsent(place.id(), place));
        }
        return List.copyOf(uniquePlaces.values());
    }

    @Transactional(readOnly = true)
    public RecommendationResponse getSession(Long sessionId, Long memberId) {
        RecommendationSession session = sessionRepository.findByIdAndMemberId(sessionId, memberId)
                .orElseThrow(() -> new BusinessException(RecommendationErrorCode.SESSION_NOT_FOUND));

        List<RecommendationCandidate> candidates = topRanked(
                candidateRepository.findBySessionIdOrderByResultRankAsc(sessionId));
        return RecommendationResponse.of(session.getId(), candidates);
    }

    private ScoredCandidate score(RestaurantCandidate candidate, CompanionType companionType) {
        boolean companionMatch = isCompanionMatch(companionType, candidate.restaurant());
        RecommendationScoreCalculator.ScoreBreakdown breakdown = scoreCalculator.calculate(
                candidate.restaurant().getExternalRating(), candidate.distanceMeters(), companionMatch);
        return new ScoredCandidate(candidate.restaurant(), candidate.distanceMeters(), breakdown);
    }

    private boolean isCompanionMatch(CompanionType companionType, Restaurant restaurant) {
        Boolean suitable = switch (companionType) {
            case DATE -> restaurant.getSuitableForDate();
            case FAMILY -> restaurant.getSuitableForFamily();
            case CHILDREN -> restaurant.getSuitableForChildren();
            case SOLO -> restaurant.getSuitableForSolo();
            case GROUP -> restaurant.getSuitableForGroup();
            case DOG -> restaurant.getSuitableForDogs();
        };
        return Boolean.TRUE.equals(suitable);
    }

    private List<RecommendationCandidate> toCandidateEntities(RecommendationSession session, List<ScoredCandidate> scored) {
        return IntStream.range(0, scored.size())
                .mapToObj(i -> {
                    ScoredCandidate candidate = scored.get(i);
                    RecommendationScoreCalculator.ScoreBreakdown breakdown = candidate.breakdown();
                    return RecommendationCandidate.builder()
                            .session(session)
                            .restaurant(candidate.restaurant())
                            .resultRank(i + 1)
                            .distanceMeters(candidate.distanceMeters())
                            .ratingContribution(breakdown.ratingContribution())
                            .distanceContribution(breakdown.distanceContribution())
                            .companionBonus(breakdown.companionBonus())
                            .totalScore(breakdown.totalScore())
                            .build();
                })
                .toList();
    }

    private record ScoredCandidate(
            Restaurant restaurant,
            double distanceMeters,
            RecommendationScoreCalculator.ScoreBreakdown breakdown
    ) {
    }
}
