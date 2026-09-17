package com.pickeat.pickeatbackend.domain.survey.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.pickeat.pickeatbackend.domain.survey.dto.SurveyCreateRequest;
import com.pickeat.pickeatbackend.domain.survey.entity.Survey;
import com.pickeat.pickeatbackend.domain.survey.entity.SurveyCategory;
import com.pickeat.pickeatbackend.domain.survey.repository.SurveyRepository;
import com.pickeat.pickeatbackend.domain.user.entity.Gender;
import com.pickeat.pickeatbackend.domain.user.entity.Job;
import com.pickeat.pickeatbackend.domain.user.entity.User;
import com.pickeat.pickeatbackend.domain.user.repository.UserRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private UserRepository userRepository;

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

    private SurveyCreateRequest request(LocalDate startDate, LocalDate endDate) {
        return new SurveyCreateRequest("점심 뭐 먹지", null, null, SurveyCategory.DAILY, null, startDate, endDate);
    }

    @Test
    void 마감일이_시작일보다_빠르면_예외가_발생한다() {
        assertThatThrownBy(() -> surveyService.create(1L,
                request(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 17))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 존재하지_않는_회원이면_예외가_발생한다() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> surveyService.create(1L,
                request(LocalDate.of(2026, 9, 17), LocalDate.of(2026, 9, 20))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 설문_생성에_성공하면_작성자가_설정된다() {
        User creator = creator();
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(surveyRepository.save(any(Survey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        surveyService.create(1L, request(LocalDate.of(2026, 9, 17), LocalDate.of(2026, 9, 20)));

        ArgumentCaptor<Survey> captor = ArgumentCaptor.forClass(Survey.class);
        org.mockito.Mockito.verify(surveyRepository).save(captor.capture());
        assertThat(captor.getValue().getCreator()).isEqualTo(creator);
        assertThat(captor.getValue().getTitle()).isEqualTo("점심 뭐 먹지");
    }
}
