package com.pickeat.pickeatbackend.domain.response.repository;

import com.pickeat.pickeatbackend.domain.response.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
}
