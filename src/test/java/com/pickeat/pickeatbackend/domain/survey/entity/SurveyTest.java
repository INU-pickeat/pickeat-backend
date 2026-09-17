package com.pickeat.pickeatbackend.domain.survey.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickeat.pickeatbackend.domain.user.entity.Gender;
import com.pickeat.pickeatbackend.domain.user.entity.Job;
import com.pickeat.pickeatbackend.domain.user.entity.User;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SurveyTest {

    @Test
    void 빌더로_생성하면_아카이브_공유와_삭제_여부는_false로_초기화된다() {
        User creator = User.builder()
                .email("test@pickeat.com")
                .password("encoded-password")
                .gender(Gender.FEMALE)
                .age(24)
                .job(Job.UNIVERSITY_STUDENT)
                .build();

        Survey survey = Survey.builder()
                .creator(creator)
                .title("점심 뭐 먹지")
                .category(SurveyCategory.DAILY)
                .startDate(LocalDate.of(2026, 9, 17))
                .endDate(LocalDate.of(2026, 9, 20))
                .build();

        assertThat(survey.isSharedToArchive()).isFalse();
        assertThat(survey.isDeleted()).isFalse();
        assertThat(survey.getTitle()).isEqualTo("점심 뭐 먹지");
    }
}
