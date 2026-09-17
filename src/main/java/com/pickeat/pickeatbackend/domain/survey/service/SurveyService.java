package com.pickeat.pickeatbackend.domain.survey.service;

import com.pickeat.pickeatbackend.domain.survey.dto.SurveyCreateRequest;
import com.pickeat.pickeatbackend.domain.survey.entity.Survey;
import com.pickeat.pickeatbackend.domain.survey.exception.SurveyErrorCode;
import com.pickeat.pickeatbackend.domain.survey.repository.SurveyRepository;
import com.pickeat.pickeatbackend.domain.user.entity.User;
import com.pickeat.pickeatbackend.domain.user.exception.UserErrorCode;
import com.pickeat.pickeatbackend.domain.user.repository.UserRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long create(Long creatorId, SurveyCreateRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException(SurveyErrorCode.INVALID_DATE_RANGE);
        }

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        Survey survey = Survey.builder()
                .creator(creator)
                .title(request.title())
                .description(request.description())
                .target(request.target())
                .category(request.category())
                .estimatedMinutes(request.estimatedMinutes())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build();

        return surveyRepository.save(survey).getId();
    }
}
