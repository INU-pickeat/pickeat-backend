package com.pickeat.pickeatbackend.domain.survey.service;

import com.pickeat.pickeatbackend.domain.survey.dto.QuestionCreateRequest;
import com.pickeat.pickeatbackend.domain.survey.dto.QuestionOptionResponse;
import com.pickeat.pickeatbackend.domain.survey.dto.QuestionResponse;
import com.pickeat.pickeatbackend.domain.survey.dto.SurveyCreateRequest;
import com.pickeat.pickeatbackend.domain.survey.dto.SurveyDetailResponse;
import com.pickeat.pickeatbackend.domain.survey.dto.SurveySummaryResponse;
import com.pickeat.pickeatbackend.domain.survey.entity.Question;
import com.pickeat.pickeatbackend.domain.survey.entity.QuestionOption;
import com.pickeat.pickeatbackend.domain.survey.entity.QuestionType;
import com.pickeat.pickeatbackend.domain.survey.entity.Survey;
import com.pickeat.pickeatbackend.domain.survey.exception.SurveyErrorCode;
import com.pickeat.pickeatbackend.domain.survey.repository.QuestionOptionRepository;
import com.pickeat.pickeatbackend.domain.survey.repository.QuestionRepository;
import com.pickeat.pickeatbackend.domain.survey.repository.SurveyRepository;
import com.pickeat.pickeatbackend.domain.user.entity.User;
import com.pickeat.pickeatbackend.domain.user.exception.UserErrorCode;
import com.pickeat.pickeatbackend.domain.user.repository.UserRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;

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
        surveyRepository.save(survey);

        int order = 1;
        for (QuestionCreateRequest questionRequest : request.questions()) {
            createQuestion(survey, order++, questionRequest);
        }

        return survey.getId();
    }

    private void createQuestion(Survey survey, int order, QuestionCreateRequest request) {
        boolean isMultipleChoice = request.type() == QuestionType.MULTIPLE_CHOICE;
        if (isMultipleChoice && (request.options() == null || request.options().isEmpty())) {
            throw new BusinessException(SurveyErrorCode.OPTIONS_REQUIRED);
        }

        Question question = Question.builder()
                .survey(survey)
                .questionOrder(order)
                .type(request.type())
                .content(request.content())
                .isRequired(request.isRequired())
                .allowMultiple(request.allowMultiple())
                .build();
        questionRepository.save(question);

        if (isMultipleChoice) {
            int optionOrder = 1;
            for (String optionContent : request.options()) {
                questionOptionRepository.save(QuestionOption.builder()
                        .question(question)
                        .optionOrder(optionOrder++)
                        .content(optionContent)
                        .build());
            }
        }
    }

    @Transactional(readOnly = true)
    public SurveyDetailResponse getDetail(Long surveyId) {
        Survey survey = surveyRepository.findById(surveyId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new BusinessException(SurveyErrorCode.SURVEY_NOT_FOUND));

        List<Question> questions = questionRepository.findBySurveyOrderByQuestionOrder(survey);
        Map<Long, List<QuestionOptionResponse>> optionsByQuestionId = questionOptionRepository
                .findByQuestionInOrderByOptionOrder(questions).stream()
                .collect(Collectors.groupingBy(
                        option -> option.getQuestion().getId(),
                        Collectors.mapping(QuestionOptionResponse::from, Collectors.toList())
                ));

        List<QuestionResponse> questionResponses = questions.stream()
                .map(question -> QuestionResponse.from(
                        question,
                        optionsByQuestionId.getOrDefault(question.getId(), List.of())
                ))
                .toList();

        return SurveyDetailResponse.from(survey, questionResponses);
    }

    @Transactional(readOnly = true)
    public Page<SurveySummaryResponse> getList(Pageable pageable) {
        return surveyRepository.findByIsDeletedFalse(pageable)
                .map(SurveySummaryResponse::from);
    }
}
