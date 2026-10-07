package com.pickeat.pickeatbackend.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pickeat.pickeatbackend.domain.member.entity.EmailVerification;
import com.pickeat.pickeatbackend.domain.member.repository.EmailVerificationRepository;
import com.pickeat.pickeatbackend.domain.member.repository.MemberRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock EmailVerificationRepository verificationRepository;
    @Mock MemberRepository memberRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JavaMailSender mailSender;

    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(
                verificationRepository, memberRepository, passwordEncoder, mailSender);
        ReflectionTestUtils.setField(service, "from", "cki08543@gmail.com");
        ReflectionTestUtils.setField(service, "codeValidityMs", 600_000L);
        ReflectionTestUtils.setField(service, "signupValidityMs", 1_800_000L);
        ReflectionTestUtils.setField(service, "resendCooldownMs", 60_000L);
    }

    @Test
    @DisplayName("6자리 인증번호를 암호화해 저장하고 메일로 보낸다")
    void sendsHashedVerificationCode() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-code");

        service.sendCode("test@pickeat.com");

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(codeCaptor.capture());
        assertThat(codeCaptor.getValue()).matches("\\d{6}");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getFrom()).isEqualTo("cki08543@gmail.com");
        assertThat(messageCaptor.getValue().getTo()).containsExactly("test@pickeat.com");
        assertThat(messageCaptor.getValue().getText()).contains(codeCaptor.getValue());
    }

    @Test
    @DisplayName("올바른 인증번호를 확인한 뒤 회원가입에서 인증을 소모한다")
    void confirmsAndConsumesVerification() {
        EmailVerification verification = verification(false);
        when(verificationRepository.findById("test@pickeat.com")).thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("123456", "encoded-code")).thenReturn(true);

        service.confirm("test@pickeat.com", "123456");
        service.consume("test@pickeat.com");

        assertThat(verification.getVerifiedAt()).isNotNull();
        verify(verificationRepository).delete(verification);
    }

    @Test
    @DisplayName("인증되지 않은 이메일은 회원가입에 사용할 수 없다")
    void rejectsUnverifiedEmailOnSignUp() {
        EmailVerification verification = verification(false);
        when(verificationRepository.findById("test@pickeat.com")).thenReturn(Optional.of(verification));

        assertThatThrownBy(() -> service.consume("test@pickeat.com"))
                .isInstanceOf(BusinessException.class);
    }

    private EmailVerification verification(boolean verified) {
        Instant now = Instant.now();
        return EmailVerification.builder()
                .email("test@pickeat.com")
                .codeHash("encoded-code")
                .sentAt(now.minusSeconds(120))
                .expiresAt(now.plusSeconds(600))
                .verifiedAt(verified ? now : null)
                .build();
    }
}
