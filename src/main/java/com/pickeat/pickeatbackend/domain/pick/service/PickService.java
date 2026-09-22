package com.pickeat.pickeatbackend.domain.pick.service;

import com.pickeat.pickeatbackend.domain.pick.dto.CreatePickRequest;
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
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PickService {

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
}
