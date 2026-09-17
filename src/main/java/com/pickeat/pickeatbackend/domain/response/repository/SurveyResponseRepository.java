package com.pickeat.pickeatbackend.domain.response.repository;

import com.pickeat.pickeatbackend.domain.response.entity.SurveyResponse;
import com.pickeat.pickeatbackend.domain.survey.entity.Survey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, Long> {

    boolean existsBySurveyAndRespondentId(Survey survey, Long respondentId);

    boolean existsBySurveyAndGuestKey(Survey survey, String guestKey);
}
