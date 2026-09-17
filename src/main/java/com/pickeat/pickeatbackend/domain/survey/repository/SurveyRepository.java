package com.pickeat.pickeatbackend.domain.survey.repository;

import com.pickeat.pickeatbackend.domain.survey.entity.Survey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyRepository extends JpaRepository<Survey, Long> {
}
