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
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// ADR-M2-2: 매 요청 실시간 Google 호출 + upsert(Option A). DB 커버리지가 쌓이면
// "DB 우선, 부족할 때만 Google" 하이브리드로 전환 — 지금은 승격 조건 미충족.
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final double SEARCH_RADIUS_METERS = 5000.0;
    private static final int TOP_RESULT_COUNT = 5;

    private final GooglePlacesClient googlePlacesClient;
    private final RestaurantService restaurantService;
    private final RestaurantRepository restaurantRepository;
    private final RecommendationScoreCalculator scoreCalculator;
    private final RecommendationSessionRepository sessionRepository;
    private final RecommendationCandidateRepository candidateRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public RecommendationResponse recommend(RecommendationRequest request, Long memberId) {
        Set<String> includedTypes = FoodCategory.toGooglePrimaryTypes(request.foodCategories());
        List<GooglePlaceResponse> places = googlePlacesClient.findNearbyRestaurants(
                request.latitude(), request.longitude(), GooglePlacesClient.RankPreference.POPULARITY, includedTypes);
        places.forEach(restaurantService::upsertFromGoogle);

        List<RestaurantCandidate> candidates = restaurantRepository.findWithinRadius(
                request.latitude(), request.longitude(), SEARCH_RADIUS_METERS);

        List<ScoredCandidate> topCandidates = candidates.stream()
                .filter(candidate -> request.foodCategories().contains(candidate.restaurant().getFoodCategory()))
                .map(candidate -> score(candidate, request.companionType()))
                .sorted(Comparator.comparingDouble((ScoredCandidate c) -> c.breakdown().totalScore()).reversed())
                .limit(TOP_RESULT_COUNT)
                .toList();

        RecommendationSession session = sessionRepository.save(RecommendationSession.builder()
                .member(memberRepository.getReferenceById(memberId))
                .companionType(request.companionType())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .foodCategories(request.foodCategories())
                .build());

        List<RecommendationCandidate> savedCandidates = candidateRepository.saveAll(
                toCandidateEntities(session, topCandidates));

        return RecommendationResponse.of(session.getId(), savedCandidates);
    }

    @Transactional(readOnly = true)
    public RecommendationResponse getSession(Long sessionId, Long memberId) {
        RecommendationSession session = sessionRepository.findById(sessionId)
                .filter(found -> found.getMember().getId().equals(memberId))
                .orElseThrow(() -> new BusinessException(RecommendationErrorCode.SESSION_NOT_FOUND));

        List<RecommendationCandidate> candidates = candidateRepository.findBySessionIdOrderByResultRankAsc(sessionId);
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
            case FRIENDS -> restaurant.getSuitableForFriends();
            case FAMILY -> restaurant.getSuitableForFamily();
            case SOLO -> restaurant.getSuitableForSolo();
            case GROUP_DINNER -> restaurant.getSuitableForGroupDinner();
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
