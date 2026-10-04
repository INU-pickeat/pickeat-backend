package com.pickeat.pickeatbackend.domain.pick.service;

import com.pickeat.pickeatbackend.domain.pick.dto.CreatePickRequest;
import com.pickeat.pickeatbackend.domain.pick.dto.PickCalendarResponse;
import com.pickeat.pickeatbackend.domain.pick.dto.PickMapResponse;
import com.pickeat.pickeatbackend.domain.pick.dto.PickPeriod;
import com.pickeat.pickeatbackend.domain.pick.dto.PickResponse;
import com.pickeat.pickeatbackend.domain.pick.dto.PickStatusUpdateRequest;
import com.pickeat.pickeatbackend.domain.pick.dto.RecentPicksResponse;
import com.pickeat.pickeatbackend.domain.pick.entity.Pick;
import com.pickeat.pickeatbackend.domain.pick.entity.PickStatus;
import com.pickeat.pickeatbackend.domain.pick.exception.PickErrorCode;
import com.pickeat.pickeatbackend.domain.pick.repository.PickRepository;
import com.pickeat.pickeatbackend.domain.recommendation.entity.RecommendationSession;
import com.pickeat.pickeatbackend.domain.recommendation.exception.RecommendationErrorCode;
import com.pickeat.pickeatbackend.domain.recommendation.repository.RecommendationCandidateRepository;
import com.pickeat.pickeatbackend.domain.recommendation.repository.RecommendationSessionRepository;
import com.pickeat.pickeatbackend.domain.restaurant.repository.RestaurantRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import com.pickeat.pickeatbackend.global.exception.GlobalErrorCode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PickService {

    // 캘린더의 "하루"와 "월" 경계는 서비스 지역인 한국 시간 기준이다.
    static final ZoneId CALENDAR_ZONE = ZoneId.of("Asia/Seoul");
    private static final int MIN_CALENDAR_YEAR = 2000;
    private static final int MAX_CALENDAR_YEAR = 2100;

    private final PickRepository pickRepository;
    private final RecommendationSessionRepository sessionRepository;
    private final RecommendationCandidateRepository candidateRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional
    public PickResponse create(CreatePickRequest request, Long memberId) {
        RecommendationSession session = sessionRepository
                .findByIdAndMemberId(request.recommendationSessionId(), memberId)
                .orElseThrow(() -> new BusinessException(RecommendationErrorCode.SESSION_NOT_FOUND));

        if (!candidateRepository.existsBySessionIdAndRestaurantId(session.getId(), request.restaurantId())) {
            throw new BusinessException(PickErrorCode.RESTAURANT_NOT_IN_SESSION);
        }
        if (pickRepository.existsByRecommendationSessionId(session.getId())) {
            throw new BusinessException(PickErrorCode.SESSION_ALREADY_PICKED);
        }

        Pick pick = Pick.builder()
                .member(session.getMember())
                .restaurant(restaurantRepository.getReferenceById(request.restaurantId()))
                .recommendationSession(session)
                .companionType(session.getCompanionType())
                .build();
        try {
            return PickResponse.from(pickRepository.saveAndFlush(pick));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(PickErrorCode.SESSION_ALREADY_PICKED);
        }
    }

    @Transactional
    public PickResponse updateStatus(Long pickId, PickStatusUpdateRequest request, Long memberId) {
        Pick pick = pickRepository.findByIdAndMemberId(pickId, memberId)
                .orElseThrow(() -> new BusinessException(PickErrorCode.PICK_NOT_FOUND));
        pick.changeStatus(request.status());
        return PickResponse.from(pick);
    }

    // 홈·내 정보용 최근 Pick 목록: 식당별로 그룹화한 SELECTED + REVIEWED 집계다.
    // Pick 캘린더(REVIEWED만, 달력 월 고정)와는 별개의 화면·집계 기준이다.
    @Transactional(readOnly = true)
    public RecentPicksResponse getMyPicks(Long memberId, PickPeriod period) {
        Instant since = Instant.now().minus(period.window());
        return RecentPicksResponse.from(pickRepository.findRecentPickSummaries(memberId, since));
    }

    // Pick 지도는 REVIEWED(방문 후기 작성 완료)만 노출한다. SELECTED·CANCELED는 미노출.
    @Transactional(readOnly = true)
    public PickMapResponse getMyPickMap(Long memberId) {
        return PickMapResponse.from(
                pickRepository.findByMemberIdAndStatusOrderBySelectedAtDescIdDesc(memberId, PickStatus.REVIEWED));
    }

    // Pick 캘린더는 REVIEWED만, 달력 월(한국 시간) 단위로 방문일(visitedAt) 기준 집계한다.
    @Transactional(readOnly = true)
    public PickCalendarResponse getMyPickCalendar(Long memberId, int year, int month) {
        if (year < MIN_CALENDAR_YEAR || year > MAX_CALENDAR_YEAR || month < 1 || month > 12) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT);
        }
        YearMonth yearMonth = YearMonth.of(year, month);
        Instant from = yearMonth.atDay(1).atStartOfDay(CALENDAR_ZONE).toInstant();
        Instant to = yearMonth.plusMonths(1).atDay(1).atStartOfDay(CALENDAR_ZONE).toInstant();
        List<Pick> picks = pickRepository
                .findByMemberIdAndStatusAndVisitedAtGreaterThanEqualAndVisitedAtLessThanOrderByVisitedAtAscIdAsc(
                        memberId, PickStatus.REVIEWED, from, to);
        return PickCalendarResponse.of(yearMonth, picks, CALENDAR_ZONE);
    }
}
