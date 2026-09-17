package com.pickeat.pickeatbackend.domain.response.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pickeat.pickeatbackend.domain.response.dto.AnswerCreateRequest;
import com.pickeat.pickeatbackend.domain.response.dto.SurveyResponseCreateRequest;
import com.pickeat.pickeatbackend.domain.response.entity.Answer;
import com.pickeat.pickeatbackend.domain.response.entity.AnswerOption;
import com.pickeat.pickeatbackend.domain.response.entity.SurveyResponse;
import com.pickeat.pickeatbackend.domain.response.repository.AnswerOptionRepository;
import com.pickeat.pickeatbackend.domain.response.repository.AnswerRepository;
import com.pickeat.pickeatbackend.domain.response.repository.SurveyResponseRepository;
import com.pickeat.pickeatbackend.domain.survey.entity.Question;
import com.pickeat.pickeatbackend.domain.survey.entity.QuestionOption;
import com.pickeat.pickeatbackend.domain.survey.entity.QuestionType;
import com.pickeat.pickeatbackend.domain.survey.entity.Survey;
import com.pickeat.pickeatbackend.domain.survey.entity.SurveyCategory;
import com.pickeat.pickeatbackend.domain.survey.repository.QuestionOptionRepository;
import com.pickeat.pickeatbackend.domain.survey.repository.QuestionRepository;
import com.pickeat.pickeatbackend.domain.survey.repository.SurveyRepository;
import com.pickeat.pickeatbackend.domain.user.entity.Gender;
import com.pickeat.pickeatbackend.domain.user.entity.Job;
import com.pickeat.pickeatbackend.domain.user.entity.User;
import com.pickeat.pickeatbackend.domain.user.repository.UserRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SurveyResponseServiceTest {

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionOptionRepository questionOptionRepository;

    @Mock
    private SurveyResponseRepository surveyResponseRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private AnswerOptionRepository answerOptionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SurveyResponseService surveyResponseService;

    private User creator() {
        User user = User.builder()
                .email("creator@pickeat.com")
                .password("encoded")
                .gender(Gender.FEMALE)
                .age(24)
                .job(Job.UNIVERSITY_STUDENT)
                .build();
        setId(user, 1L);
        return user;
    }

    private User respondent() {
        User user = User.builder()
                .email("respondent@pickeat.com")
                .password("encoded")
                .gender(Gender.MALE)
                .age(25)
                .job(Job.EMPLOYEE)
                .build();
        setId(user, 2L);
        return user;
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Survey survey() {
        Survey survey = Survey.builder()
                .creator(creator())
                .title("점심 뭐 먹지")
                .category(SurveyCategory.DAILY)
                .startDate(LocalDate.of(2026, 9, 17))
                .endDate(LocalDate.of(2026, 9, 20))
                .build();
        setId(survey, 10L);
        return survey;
    }

    private Question subjectiveQuestion(Survey survey, boolean required) {
        Question question = Question.builder()
                .survey(survey)
                .questionOrder(1)
                .type(QuestionType.SUBJECTIVE)
                .content("좋아하는 메뉴는?")
                .isRequired(required)
                .allowMultiple(false)
                .build();
        setId(question, 100L);
        return question;
    }

    @Test
    void 존재하지_않는_설문에_참여하면_예외가_발생한다() {
        when(surveyRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> surveyResponseService.submit(10L, 2L,
                new SurveyResponseCreateRequest(null, List.of())))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 본인_설문에_참여하면_예외가_발생한다() {
        Survey survey = survey();
        when(surveyRepository.findById(10L)).thenReturn(Optional.of(survey));

        assertThatThrownBy(() -> surveyResponseService.submit(10L, 1L,
                new SurveyResponseCreateRequest(null, List.of())))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 이미_참여한_회원이면_예외가_발생한다() {
        Survey survey = survey();
        when(surveyRepository.findById(10L)).thenReturn(Optional.of(survey));
        when(surveyResponseRepository.existsBySurveyAndRespondentId(survey, 2L)).thenReturn(true);

        assertThatThrownBy(() -> surveyResponseService.submit(10L, 2L,
                new SurveyResponseCreateRequest(null, List.of())))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 게스트인데_guestKey가_없으면_예외가_발생한다() {
        Survey survey = survey();
        when(surveyRepository.findById(10L)).thenReturn(Optional.of(survey));

        assertThatThrownBy(() -> surveyResponseService.submit(10L, null,
                new SurveyResponseCreateRequest(null, List.of())))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 필수_문항에_답하지_않으면_예외가_발생한다() {
        Survey survey = survey();
        Question question = subjectiveQuestion(survey, true);
        when(surveyRepository.findById(10L)).thenReturn(Optional.of(survey));
        when(surveyResponseRepository.existsBySurveyAndRespondentId(survey, 2L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(respondent()));
        when(questionRepository.findBySurveyOrderByQuestionOrder(survey)).thenReturn(List.of(question));
        when(questionOptionRepository.findByQuestionInOrderByOptionOrder(List.of(question))).thenReturn(List.of());
        when(surveyResponseRepository.save(any(SurveyResponse.class))).thenAnswer(i -> i.getArgument(0));

        assertThatThrownBy(() -> surveyResponseService.submit(10L, 2L,
                new SurveyResponseCreateRequest(null, List.of())))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 회원이_정상_제출하면_답변이_저장된다() {
        Survey survey = survey();
        Question question = subjectiveQuestion(survey, true);
        when(surveyRepository.findById(10L)).thenReturn(Optional.of(survey));
        when(surveyResponseRepository.existsBySurveyAndRespondentId(survey, 2L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(respondent()));
        when(questionRepository.findBySurveyOrderByQuestionOrder(survey)).thenReturn(List.of(question));
        when(questionOptionRepository.findByQuestionInOrderByOptionOrder(List.of(question))).thenReturn(List.of());
        when(surveyResponseRepository.save(any(SurveyResponse.class))).thenAnswer(i -> i.getArgument(0));
        when(answerRepository.save(any(Answer.class))).thenAnswer(i -> i.getArgument(0));

        surveyResponseService.submit(10L, 2L,
                new SurveyResponseCreateRequest(null, List.of(
                        new AnswerCreateRequest(100L, "한식이요", null)
                )));

        ArgumentCaptor<Answer> answerCaptor = ArgumentCaptor.forClass(Answer.class);
        verify(answerRepository).save(answerCaptor.capture());
        assertThat(answerCaptor.getValue().getAnswerText()).isEqualTo("한식이요");
        assertThat(answerCaptor.getValue().getQuestion()).isEqualTo(question);
    }

    @Test
    void 객관식_보기가_문항에_속하지_않으면_예외가_발생한다() {
        Survey survey = survey();
        Question question = Question.builder()
                .survey(survey)
                .questionOrder(1)
                .type(QuestionType.MULTIPLE_CHOICE)
                .content("뭐 먹을래?")
                .isRequired(true)
                .allowMultiple(false)
                .build();
        setId(question, 200L);

        QuestionOption option = QuestionOption.builder()
                .question(question)
                .optionOrder(1)
                .content("한식")
                .build();
        setId(option, 300L);

        when(surveyRepository.findById(10L)).thenReturn(Optional.of(survey));
        when(surveyResponseRepository.existsBySurveyAndRespondentId(survey, 2L)).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(respondent()));
        when(questionRepository.findBySurveyOrderByQuestionOrder(survey)).thenReturn(List.of(question));
        when(questionOptionRepository.findByQuestionInOrderByOptionOrder(List.of(question)))
                .thenReturn(List.of(option));
        when(surveyResponseRepository.save(any(SurveyResponse.class))).thenAnswer(i -> i.getArgument(0));
        when(answerRepository.save(any(Answer.class))).thenAnswer(i -> i.getArgument(0));

        assertThatThrownBy(() -> surveyResponseService.submit(10L, 2L,
                new SurveyResponseCreateRequest(null, List.of(
                        new AnswerCreateRequest(200L, null, List.of(999L))
                ))))
                .isInstanceOf(BusinessException.class);
    }
}
