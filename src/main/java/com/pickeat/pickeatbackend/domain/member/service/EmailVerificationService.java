package com.pickeat.pickeatbackend.domain.member.service;

import com.pickeat.pickeatbackend.domain.member.entity.EmailVerification;
import com.pickeat.pickeatbackend.domain.member.exception.MemberErrorCode;
import com.pickeat.pickeatbackend.domain.member.repository.EmailVerificationRepository;
import com.pickeat.pickeatbackend.domain.member.repository.MemberRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.security.SecureRandom;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationRepository verificationRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${email-verification.from}")
    private String from;

    @Value("${email-verification.code-validity-ms}")
    private long codeValidityMs;

    @Value("${email-verification.signup-validity-ms}")
    private long signupValidityMs;

    @Value("${email-verification.resend-cooldown-ms}")
    private long resendCooldownMs;

    @Transactional
    public void sendCode(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(MemberErrorCode.DUPLICATE_EMAIL);
        }
        issueCode(email, "[Pick Eat] 이메일 인증번호", "Pick Eat 이메일 인증번호는 %s입니다. 10분 안에 입력해 주세요.");
    }

    // 가입한 이메일에만 보낸다. 가입용 인증은 미가입 이메일에만 생기므로 같은 테이블·확인 API를 써도
    // 두 용도가 섞이지 않는다. 확인은 POST /auth/email-verifications/confirm을 그대로 쓴다.
    @Transactional
    public void sendPasswordResetCode(String email) {
        if (!memberRepository.existsByEmail(email)) {
            throw new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND);
        }
        issueCode(email, "[Pick Eat] 비밀번호 재설정 인증번호",
                "Pick Eat 비밀번호 재설정 인증번호는 %s입니다. 10분 안에 입력해 주세요.");
    }

    private void issueCode(String email, String subject, String textFormat) {
        Instant now = Instant.now();
        EmailVerification verification = verificationRepository.findById(email).orElse(null);
        if (verification != null && verification.getSentAt().plusMillis(resendCooldownMs).isAfter(now)) {
            throw new BusinessException(MemberErrorCode.EMAIL_VERIFICATION_TOO_FREQUENT);
        }

        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        if (verification == null) {
            verification = EmailVerification.builder()
                    .email(email)
                    .codeHash(passwordEncoder.encode(code))
                    .sentAt(now)
                    .expiresAt(now.plusMillis(codeValidityMs))
                    .build();
        } else {
            verification.replaceCode(passwordEncoder.encode(code), now, now.plusMillis(codeValidityMs));
        }
        verificationRepository.saveAndFlush(verification);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(subject);
        message.setText(textFormat.formatted(code));
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new BusinessException(MemberErrorCode.EMAIL_DELIVERY_FAILED);
        }
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public void confirm(String email, String code) {
        EmailVerification verification = verificationRepository.findById(email)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.EMAIL_VERIFICATION_REQUIRED));
        Instant now = Instant.now();

        if (verification.isExpired(now)) {
            verificationRepository.delete(verification);
            throw new BusinessException(MemberErrorCode.EXPIRED_EMAIL_VERIFICATION_CODE);
        }
        if (verification.isVerified(now)) {
            return;
        }
        if (verification.getFailedAttempts() >= MAX_FAILED_ATTEMPTS
                || !passwordEncoder.matches(code, verification.getCodeHash())) {
            verification.recordFailedAttempt();
            throw new BusinessException(MemberErrorCode.INVALID_EMAIL_VERIFICATION_CODE);
        }
        verification.markVerified(now, now.plusMillis(signupValidityMs));
    }

    @Transactional
    public void consume(String email) {
        EmailVerification verification = verificationRepository.findById(email)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.EMAIL_VERIFICATION_REQUIRED));
        if (!verification.isVerified(Instant.now())) {
            throw new BusinessException(MemberErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }
        verificationRepository.delete(verification);
    }
}
