package com.pickeat.pickeatbackend.domain.response.service;

import com.pickeat.pickeatbackend.domain.response.dto.AnswerCreateRequest;
import com.pickeat.pickeatbackend.domain.response.dto.SurveyResponseCreateRequest;
import com.pickeat.pickeatbackend.domain.response.entity.Answer;
import com.pickeat.pickeatbackend.domain.response.entity.AnswerOption;
import com.pickeat.pickeatbackend.domain.response.entity.SurveyResponse;
import com.pickeat.pickeatbackend.domain.response.exception.ResponseErrorCode;
import com.pickeat.pickeatbackend.domain.response.repository.AnswerOptionRepository;
import com.pickeat.pickeatbackend.domain.response.repository.AnswerRepository;
import com.pickeat.pickeatbackend.domain.response.repository.SurveyResponseRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SurveyResponseService {

    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final AnswerRepository answerRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long submit(Long surveyId, Long respondentId, SurveyResponseCreateRequest request) {
        Survey survey = surveyRepository.findById(surveyId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new BusinessException(SurveyErrorCode.SURVEY_NOT_FOUND));

        User respondent = null;
        String guestKey = null;

        if (respondentId != null) {
            if (survey.getCreator().getId().equals(respondentId)) {
                throw new BusinessException(ResponseErrorCode.CANNOT_RESPOND_OWN_SURVEY);
            }
            if (surveyResponseRepository.existsBySurveyAndRespondentId(survey, respondentId)) {
                throw new BusinessException(ResponseErrorCode.DUPLICATE_RESPONSE);
            }
            respondent = userRepository.findById(respondentId)
                    .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        } else {
            guestKey = request.guestKey();
            if (guestKey == null || guestKey.isBlank()) {
                throw new BusinessException(ResponseErrorCode.GUEST_KEY_REQUIRED);
            }
            if (surveyResponseRepository.existsBySurveyAndGuestKey(survey, guestKey)) {
                throw new BusinessException(ResponseErrorCode.DUPLICATE_RESPONSE);
            }
        }

        List<Question> questions = questionRepository.findBySurveyOrderByQuestionOrder(survey);
        Map<Long, List<QuestionOption>> optionsByQuestionId = questionOptionRepository
                .findByQuestionInOrderByOptionOrder(questions).stream()
                .collect(Collectors.groupingBy(option -> option.getQuestion().getId()));
        Map<Long, AnswerCreateRequest> answersByQuestionId = request.answers() == null
                ? Map.of()
                : request.answers().stream()
                        .collect(Collectors.toMap(AnswerCreateRequest::questionId, a -> a));

        SurveyResponse response = SurveyResponse.builder()
                .survey(survey)
                .respondent(respondent)
                .guestKey(guestKey)
                .build();
        surveyResponseRepository.save(response);

        for (Question question : questions) {
            AnswerCreateRequest answerRequest = answersByQuestionId.get(question.getId());

            if (answerRequest == null) {
                if (question.isRequired()) {
                    throw new BusinessException(ResponseErrorCode.REQUIRED_QUESTION_NOT_ANSWERED);
                }
                continue;
            }

            if (question.getType() == QuestionType.SUBJECTIVE) {
                saveSubjectiveAnswer(response, question, answerRequest);
            } else {
                saveMultipleChoiceAnswer(response, question, answerRequest, optionsByQuestionId);
            }
        }

        return response.getId();
    }

    private void saveSubjectiveAnswer(SurveyResponse response, Question question, AnswerCreateRequest answerRequest) {
        if (answerRequest.answerText() == null || answerRequest.answerText().isBlank()) {
            throw new BusinessException(ResponseErrorCode.INVALID_ANSWER);
        }

        answerRepository.save(Answer.builder()
                .response(response)
                .question(question)
                .answerText(answerRequest.answerText())
                .build());
    }

    private void saveMultipleChoiceAnswer(
            SurveyResponse response,
            Question question,
            AnswerCreateRequest answerRequest,
            Map<Long, List<QuestionOption>> optionsByQuestionId
    ) {
        List<Long> optionIds = answerRequest.optionIds();
        if (optionIds == null || optionIds.isEmpty()) {
            throw new BusinessException(ResponseErrorCode.INVALID_ANSWER);
        }
        if (!question.isAllowMultiple() && optionIds.size() > 1) {
            throw new BusinessException(ResponseErrorCode.INVALID_ANSWER);
        }

        Map<Long, QuestionOption> validOptionById = optionsByQuestionId
                .getOrDefault(question.getId(), List.of()).stream()
                .collect(Collectors.toMap(QuestionOption::getId, option -> option));

        Answer answer = Answer.builder()
                .response(response)
                .question(question)
                .build();
        answerRepository.save(answer);

        for (Long optionId : optionIds) {
            QuestionOption option = validOptionById.get(optionId);
            if (option == null) {
                throw new BusinessException(ResponseErrorCode.INVALID_ANSWER);
            }
            answerOptionRepository.save(AnswerOption.builder()
                    .answer(answer)
                    .option(option)
                    .build());
        }
    }
}
