package com.pickeat.pickeatbackend.domain.survey.repository;

import com.pickeat.pickeatbackend.domain.survey.entity.Question;
import com.pickeat.pickeatbackend.domain.survey.entity.Survey;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findBySurveyOrderByQuestionOrder(Survey survey);
}
