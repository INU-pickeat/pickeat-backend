package com.pickeat.pickeatbackend.domain.recommendation.service;

import com.pickeat.pickeatbackend.domain.member.repository.MemberRepository;
import com.pickeat.pickeatbackend.domain.recommendation.dto.RecommendationExclusionRequest;
import com.pickeat.pickeatbackend.domain.recommendation.dto.RecommendationRequest;
import com.pickeat.pickeatbackend.domain.recommendation.dto.RecommendationResponse;
import com.pickeat.pickeatbackend.domain.recommendation.entity.CompanionType;
import com.pickeat.pickeatbackend.domain.recommendation.entity.RecommendationCandidate;
import com.pickeat.pickeatbackend.domain.recommendation.entity.RecommendationExclusion;
import com.pickeat.pickeatbackend.domain.recommendation.entity.RecommendationSession;
import com.pickeat.pickeatbackend.domain.recommendation.exception.RecommendationErrorCode;
import com.pickeat.pickeatbackend.domain.recommendation.repository.RecommendationCandidateRepository;
import com.pickeat.pickeatbackend.domain.recommendation.repository.RecommendationExclusionRepository;
import com.pickeat.pickeatbackend.domain.recommendation.repository.RecommendationSessionRepository;
import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlaceResponse;
import com.pickeat.pickeatbackend.domain.restaurant.client.GooglePlacesClient;
import com.pickeat.pickeatbackend.domain.restaurant.entity.FoodCategory;
import com.pickeat.pickeatbackend.domain.restaurant.entity.Restaurant;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantCandidate;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import com.pickeat.pickeatbackend.domain.restaurant.service.FranchiseBrands;
import com.pickeat.pickeatbackend.domain.restaurant.service.OpeningHours;
import com.pickeat.pickeatbackend.domain.restaurant.service.RestaurantService;
import com.pickeat.pickeatbackend.domain.review.service.ReviewSummaryService;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// DB 우선, 부족할 때만 Google 하이브리드(M2 계약 개편). DB에서 카테고리·가격·데이트 프랜차이즈
// 필터를 통과한 유효 후보가 합계 10개 미만이거나, 요청한 카테고리 중 5개 미만인 카테고리가 있으면
// 부족한 카테고리만 Google Places로 보충한다. 상위 10개를 세션 후보로
// 저장하고(1~5위 노출, 6~10위는 제외 시 대체), 응답에는 상위 5개만 반환한다.
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final double DENSE_AREA_RADIUS_METERS = 1000.0;
    private static final double DEFAULT_RADIUS_METERS = 5000.0;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Duration ARRIVAL_MARGIN = Duration.ofMinutes(30);
    private static final int TOP_RESULT_COUNT = 5;
    private static final int STORED_CANDIDATE_COUNT = 10;
    private static final int MIN_DB_CANDIDATES_BEFORE_GOOGLE = 10;
    private static final int MIN_DB_CANDIDATES_PER_CATEGORY = 5;
    private static final Duration GOOGLE_FILL_COOLDOWN = Duration.ofHours(6);

    // 같은 구역·카테고리를 최근에 Google로 보충했는지 기억한다. 식당이 원래 적은 구역에서 요청마다
    // Google을 다시 부르지 않기 위한 것이다. 키는 "카테고리:위도*100:경도*100"(약 1km 격자).
    // ponytail: 인스턴스 메모리라 재시작하면 비워지고 인스턴스가 여러 대면 공유되지 않는다. 키를 지우지 않아
    // 격자 수만큼 늘어난다. 서버를 늘리거나 지역이 넓어지면 DB 테이블이나 TTL 캐시로 옮긴다.
    private final Map<String, Instant> lastGoogleFillAt = new ConcurrentHashMap<>();

    private final GooglePlacesClient googlePlacesClient;
    private final RestaurantService restaurantService;
    private final RestaurantRepository restaurantRepository;
    private final RecommendationScoreCalculator scoreCalculator;
    private final RecommendationSessionRepository sessionRepository;
    private final RecommendationCandidateRepository candidateRepository;
    private final RecommendationExclusionRepository exclusionRepository;
    private final MemberRepository memberRepository;
    private final ReviewSummaryService reviewSummaryService;

    @Transactional
    public RecommendationResponse recommend(RecommendationRequest request, Long memberId) {
        double radiusMeters = searchRadiusMeters(request.latitude(), request.longitude());
        List<RestaurantCandidate> candidates = findValidCandidates(request, radiusMeters);

        Set<FoodCategory> categoriesToFill = categoriesToFill(request, candidates);
        if (!categoriesToFill.isEmpty()) {
            Set<String> includedTypes = FoodCategory.toGooglePrimaryTypes(categoriesToFill);
            findGooglePlaces(request, radiusMeters, includedTypes).forEach(restaurantService::upsertFromGoogle);
            Instant now = Instant.now();
            categoriesToFill.forEach(category -> lastGoogleFillAt.put(fillKey(request, category), now));
            candidates = findValidCandidates(request, radiusMeters);
        }

        List<ScoredCandidate> topCandidates = candidates.stream()
                .map(candidate -> score(candidate, request.companionType(), radiusMeters))
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

        return toResponse(session.getId(), topRanked(savedCandidates));
    }

    // Google로 보충할 카테고리를 고른다. 합계가 10개 미만이면 요청한 카테고리 전부, 아니면 후보가
    // 5개 미만인 카테고리만. 합계만 보면 한 카테고리로 10개가 채워진 지역에서 나머지 카테고리를 영영
    // 가져오지 않는다. 최근에 보충한 구역·카테고리는 건너뛴다.
    private Set<FoodCategory> categoriesToFill(RecommendationRequest request, List<RestaurantCandidate> candidates) {
        // 보조 카테고리가 있는 식당은 두 카테고리 모두에 센다.
        Map<FoodCategory, Long> countByCategory = request.foodCategories().stream()
                .collect(Collectors.toMap(
                        category -> category,
                        category -> candidates.stream()
                                .filter(c -> c.restaurant().servesAnyOf(Set.of(category)))
                                .count()));
        boolean totalShort = candidates.size() < MIN_DB_CANDIDATES_BEFORE_GOOGLE;
        Instant cooldownStart = Instant.now().minus(GOOGLE_FILL_COOLDOWN);
        return request.foodCategories().stream()
                .filter(category -> totalShort
                        || countByCategory.getOrDefault(category, 0L) < MIN_DB_CANDIDATES_PER_CATEGORY)
                .filter(category -> {
                    Instant last = lastGoogleFillAt.get(fillKey(request, category));
                    return last == null || last.isBefore(cooldownStart);
                })
                .collect(Collectors.toUnmodifiableSet());
    }

    private String fillKey(RecommendationRequest request, FoodCategory category) {
        return category + ":" + Math.round(request.latitude() * 100) + ":" + Math.round(request.longitude() * 100);
    }

    // 수도권(서울·인천·경기)과 부산은 식당 밀도가 높아 1km, 그 외 지역은 5km 반경을 쓴다.
    // ponytail: 위경도 사각형 근사라 경계 인접 지역(춘천 서부·철원, 김해·양산 일부 등)이 1km로 잡힐 수 있다.
    // 정확도가 필요해지면 행정구역 폴리곤이나 역지오코딩으로 교체한다.
    static double searchRadiusMeters(double latitude, double longitude) {
        boolean metropolitan = latitude >= 36.89 && latitude <= 38.30 && longitude >= 126.30 && longitude <= 127.70;
        boolean busan = latitude >= 34.88 && latitude <= 35.39 && longitude >= 128.76 && longitude <= 129.31;
        return metropolitan || busan ? DENSE_AREA_RADIUS_METERS : DEFAULT_RADIUS_METERS;
    }

    // DB에서 반경 이내 후보를 조회한 뒤 카테고리·가격 조건을 통과한 후보만 남긴다.
    // 동행이 DATE면 프랜차이즈도 뺀다. 다른 동행 유형에는 적용하지 않는다.
    // 30분 뒤에 영업 중인 식당만 남긴다(마감 30분 전부터 제외, 30분 안에 열면 포함). 영업시간을 모르면 남긴다.
    private List<RestaurantCandidate> findValidCandidates(RecommendationRequest request, double radiusMeters) {
        ZonedDateTime arrival = ZonedDateTime.now(SEOUL).plus(ARRIVAL_MARGIN);
        return restaurantRepository.findWithinRadius(request.latitude(), request.longitude(), radiusMeters)
                .stream()
                .filter(candidate -> OpeningHours.isOpenAt(candidate.restaurant().getOpeningWeekMinutes(), arrival))
                .filter(candidate -> candidate.restaurant().servesAnyOf(request.foodCategories()))
                .filter(candidate -> matchesPriceRange(candidate.restaurant(), request.priceRange()))
                .filter(candidate -> request.companionType() != CompanionType.DATE
                        || !FranchiseBrands.isFranchise(candidate.restaurant().getName()))
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

    private List<GooglePlaceResponse> findGooglePlaces(
            RecommendationRequest request, double radiusMeters, Set<String> includedTypes) {
        List<String> types = new ArrayList<>(includedTypes);
        Map<String, GooglePlaceResponse> uniquePlaces = new LinkedHashMap<>();
        for (int start = 0; start < types.size(); start += 50) {
            Set<String> batch = Set.copyOf(types.subList(start, Math.min(start + 50, types.size())));
            googlePlacesClient.findNearbyRestaurants(
                            request.latitude(), request.longitude(), radiusMeters,
                            GooglePlacesClient.RankPreference.POPULARITY, batch)
                    .forEach(place -> uniquePlaces.putIfAbsent(place.id(), place));
        }
        return List.copyOf(uniquePlaces.values());
    }

    @Transactional(readOnly = true)
    public RecommendationResponse getSession(Long sessionId, Long memberId) {
        RecommendationSession session = sessionRepository.findByIdAndMemberId(sessionId, memberId)
                .orElseThrow(() -> new BusinessException(RecommendationErrorCode.SESSION_NOT_FOUND));

        return toResponse(session.getId(), visibleCandidates(sessionId));
    }

    // 제외는 해당 세션 안에서만 유효하다(영구 차단 아님). 대체 후보는 세션 생성 시 저장해 둔
    // 상위 10개 중 6~10위에서만 채우며, 소진되면 반경을 넓히지 않고 5개보다 적게 노출한다.
    @Transactional
    public RecommendationResponse exclude(Long sessionId, Long memberId, RecommendationExclusionRequest request) {
        RecommendationSession session = sessionRepository.findByIdAndMemberId(sessionId, memberId)
                .orElseThrow(() -> new BusinessException(RecommendationErrorCode.SESSION_NOT_FOUND));

        Long restaurantId = request.restaurantId();
        if (!candidateRepository.existsBySessionIdAndRestaurantId(sessionId, restaurantId)) {
            throw new BusinessException(RecommendationErrorCode.CANDIDATE_NOT_FOUND);
        }

        if (!exclusionRepository.existsBySessionIdAndRestaurantId(sessionId, restaurantId)) {
            exclusionRepository.save(RecommendationExclusion.builder()
                    .session(session)
                    .restaurant(restaurantRepository.getReferenceById(restaurantId))
                    .reason(request.reason())
                    .build());
        }

        return toResponse(session.getId(), visibleCandidates(sessionId));
    }

    private RecommendationResponse toResponse(Long sessionId, List<RecommendationCandidate> candidates) {
        List<Long> restaurantIds = candidates.stream().map(candidate -> candidate.getRestaurant().getId()).toList();
        return RecommendationResponse.of(sessionId, candidates, reviewSummaryService.getOneLineReviews(restaurantIds));
    }

    private List<RecommendationCandidate> visibleCandidates(Long sessionId) {
        Set<Long> excludedRestaurantIds = exclusionRepository.findBySessionId(sessionId).stream()
                .map(exclusion -> exclusion.getRestaurant().getId())
                .collect(Collectors.toSet());

        return candidateRepository.findBySessionIdOrderByResultRankAsc(sessionId).stream()
                .filter(candidate -> !excludedRestaurantIds.contains(candidate.getRestaurant().getId()))
                .limit(TOP_RESULT_COUNT)
                .toList();
    }

    private ScoredCandidate score(RestaurantCandidate candidate, CompanionType companionType, double radiusMeters) {
        boolean companionMatch = isCompanionMatch(companionType, candidate.restaurant());
        RecommendationScoreCalculator.ScoreBreakdown breakdown = scoreCalculator.calculate(
                candidate.restaurant().getExternalRating(), candidate.restaurant().getExternalRatingCount(),
                candidate.distanceMeters(), radiusMeters, companionMatch);
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
        // 팀이 입력한 적합도가 없으면 이름의 메뉴 키워드로 추정한다(예: 파스타 → 데이트 적합, 라멘 → 혼밥 적합).
        if (suitable == null) {
            return CompanionNameHint.find(restaurant.getName(), companionType).orElse(false);
        }
        return suitable;
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
