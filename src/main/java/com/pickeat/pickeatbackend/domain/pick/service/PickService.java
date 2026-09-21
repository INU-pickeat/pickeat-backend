package com.pickeat.pickeatbackend.domain.pick.service;

import com.pickeat.pickeatbackend.domain.pick.dto.CreatePickRequest;
import com.pickeat.pickeatbackend.domain.pick.dto.PickListResponse;
import com.pickeat.pickeatbackend.domain.pick.dto.PickMapResponse;
import com.pickeat.pickeatbackend.domain.pick.dto.PickResponse;
import com.pickeat.pickeatbackend.domain.pick.dto.PickStatusUpdateRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    @Transactional(readOnly = true)
    public PickListResponse getMyPicks(Long memberId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("selectedAt"), Sort.Order.desc("id")));
        return PickListResponse.from(pickRepository.findByMemberId(memberId, pageable));
    }

    @Transactional(readOnly = true)
    public PickMapResponse getMyPickMap(Long memberId) {
        return PickMapResponse.from(
                pickRepository.findByMemberIdAndStatusNotOrderBySelectedAtDescIdDesc(memberId, PickStatus.CANCELED));
    }
}
