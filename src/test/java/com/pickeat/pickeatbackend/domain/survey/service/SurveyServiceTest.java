package com.pickeat.pickeatbackend.domain.survey.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pickeat.pickeatbackend.domain.survey.dto.QuestionCreateRequest;
import com.pickeat.pickeatbackend.domain.survey.dto.SurveyCreateRequest;
import com.pickeat.pickeatbackend.domain.survey.dto.SurveyDetailResponse;
import com.pickeat.pickeatbackend.domain.survey.dto.SurveySummaryResponse;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionOptionRepository questionOptionRepository;

    @InjectMocks
    private SurveyService surveyService;

    private User creator() {
        return User.builder()
                .email("test@pickeat.com")
                .password("encoded-password")
                .gender(Gender.FEMALE)
                .age(24)
                .job(Job.UNIVERSITY_STUDENT)
                .build();
    }

    private QuestionCreateRequest subjectiveQuestion() {
        return new QuestionCreateRequest(QuestionType.SUBJECTIVE, "좋아하는 메뉴는?", false, false, null);
    }

    private QuestionCreateRequest multipleChoiceQuestion(List<String> options) {
        return new QuestionCreateRequest(QuestionType.MULTIPLE_CHOICE, "선호하는 시간은?", true, false, options);
    }

    private SurveyCreateRequest request(LocalDate startDate, LocalDate endDate, List<QuestionCreateRequest> questions) {
        return new SurveyCreateRequest(
                "점심 뭐 먹지", null, null, SurveyCategory.DAILY, null, startDate, endDate, questions
        );
    }

    @Test
    void 마감일이_시작일보다_빠르면_예외가_발생한다() {
        assertThatThrownBy(() -> surveyService.create(1L,
                request(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 17), List.of(subjectiveQuestion()))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 존재하지_않는_회원이면_예외가_발생한다() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> surveyService.create(1L,
                request(LocalDate.of(2026, 9, 17), LocalDate.of(2026, 9, 20), List.of(subjectiveQuestion()))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 객관식_문항에_보기가_없으면_예외가_발생한다() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator()));

        assertThatThrownBy(() -> surveyService.create(1L,
                request(LocalDate.of(2026, 9, 17), LocalDate.of(2026, 9, 20),
                        List.of(multipleChoiceQuestion(List.of())))))
                .isInstanceOf(BusinessException.class);

        verify(questionOptionRepository, never()).save(any());
    }

    @Test
    void 설문_생성에_성공하면_문항과_보기가_함께_저장된다() {
        User creator = creator();
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));

        surveyService.create(1L, request(
                LocalDate.of(2026, 9, 17), LocalDate.of(2026, 9, 20),
                List.of(subjectiveQuestion(), multipleChoiceQuestion(List.of("아침", "점심", "저녁")))
        ));

        ArgumentCaptor<Survey> surveyCaptor = ArgumentCaptor.forClass(Survey.class);
        verify(surveyRepository).save(surveyCaptor.capture());
        assertThat(surveyCaptor.getValue().getCreator()).isEqualTo(creator);

        verify(questionRepository, times(2)).save(any(Question.class));
        verify(questionOptionRepository, times(3)).save(any(QuestionOption.class));
    }

    private Survey survey() {
        return Survey.builder()
                .creator(creator())
                .title("점심 뭐 먹지")
                .category(SurveyCategory.DAILY)
                .startDate(LocalDate.of(2026, 9, 17))
                .endDate(LocalDate.of(2026, 9, 20))
                .build();
    }

    @Test
    void 존재하지_않는_설문을_조회하면_예외가_발생한다() {
        when(surveyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> surveyService.getDetail(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 삭제된_설문을_조회하면_예외가_발생한다() {
        Survey deleted = survey();
        deleted.markAsDeleted();
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> surveyService.getDetail(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 설문을_조회하면_문항과_함께_상세_정보를_반환한다() {
        Survey survey = survey();
        when(surveyRepository.findById(1L)).thenReturn(Optional.of(survey));
        when(questionRepository.findBySurveyOrderByQuestionOrder(survey)).thenReturn(List.of());
        when(questionOptionRepository.findByQuestionInOrderByOptionOrder(List.of())).thenReturn(List.of());

        SurveyDetailResponse response = surveyService.getDetail(1L);

        assertThat(response.title()).isEqualTo("점심 뭐 먹지");
        assertThat(response.category()).isEqualTo(SurveyCategory.DAILY);
        assertThat(response.questions()).isEmpty();
    }

    @Test
    void 설문_목록을_조회하면_삭제되지_않은_설문만_요약으로_반환한다() {
        Pageable pageable = PageRequest.of(0, 20);
        when(surveyRepository.findByIsDeletedFalse(pageable))
                .thenReturn(new PageImpl<>(List.of(survey()), pageable, 1));

        List<SurveySummaryResponse> content = surveyService.getList(pageable).getContent();

        assertThat(content).hasSize(1);
        assertThat(content.get(0).title()).isEqualTo("점심 뭐 먹지");
    }
}
