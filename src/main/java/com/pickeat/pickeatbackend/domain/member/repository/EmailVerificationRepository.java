package com.pickeat.pickeatbackend.domain.member.repository;

import com.pickeat.pickeatbackend.domain.member.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, String> {
}
