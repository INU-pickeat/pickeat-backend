package com.pickeat.pickeatbackend.domain.survey.repository;

import com.pickeat.pickeatbackend.domain.survey.entity.Question;
import com.pickeat.pickeatbackend.domain.survey.entity.QuestionOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {

    List<QuestionOption> findByQuestionInOrderByOptionOrder(List<Question> questions);
}
