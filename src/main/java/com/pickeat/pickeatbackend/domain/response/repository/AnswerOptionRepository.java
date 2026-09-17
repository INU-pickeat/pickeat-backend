package com.pickeat.pickeatbackend.domain.response.repository;

import com.pickeat.pickeatbackend.domain.response.entity.AnswerOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Long> {
}
