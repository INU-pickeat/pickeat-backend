package com.pickeat.pickeatbackend.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pickeat.pickeatbackend.domain.member.dto.LoginRequest;
import com.pickeat.pickeatbackend.domain.member.dto.LoginResponse;
import com.pickeat.pickeatbackend.domain.member.dto.MemberProfileResponse;
import com.pickeat.pickeatbackend.domain.member.dto.SignUpRequest;
import com.pickeat.pickeatbackend.domain.member.dto.UpdateProfileRequest;
import com.pickeat.pickeatbackend.domain.member.entity.Member;
import com.pickeat.pickeatbackend.domain.member.repository.MemberRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private MemberService memberService;

    private SignUpRequest request() {
        return new SignUpRequest("test@pickeat.com", "password123", "픽잇러");
    }

    private Member savedMember() {
        return Member.builder()
                .email("test@pickeat.com")
                .password("encoded-password")
                .nickname("픽잇러")
                .build();
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 예외가 발생한다")
    void throwsWhenEmailAlreadyExists() {
        when(memberRepository.existsByEmail("test@pickeat.com")).thenReturn(true);

        assertThatThrownBy(() -> memberService.signUp(request()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("회원가입에 성공하면 비밀번호는 암호화되어 저장된다")
    void encodesPasswordOnSignUp() {
        when(memberRepository.existsByEmail("test@pickeat.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        memberService.signUp(request());

        verify(emailVerificationService).consume("test@pickeat.com");
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded-password");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인하면 예외가 발생한다")
    void throwsWhenLoginEmailNotFound() {
        when(memberRepository.findByEmail("test@pickeat.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.login(new LoginRequest("test@pickeat.com", "password123")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 예외가 발생한다")
    void throwsWhenPasswordDoesNotMatch() {
        when(memberRepository.findByEmail("test@pickeat.com")).thenReturn(Optional.of(savedMember()));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> memberService.login(new LoginRequest("test@pickeat.com", "wrong-password")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("로그인에 성공하면 액세스 토큰과 리프레시 토큰을 반환한다")
    void returnsTokensOnSuccessfulLogin() {
        when(memberRepository.findByEmail("test@pickeat.com")).thenReturn(Optional.of(savedMember()));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(authTokenService.issue(any(Member.class))).thenReturn(new LoginResponse("access-token", "refresh-token"));

        LoginResponse response = memberService.login(new LoginRequest("test@pickeat.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("존재하지 않는 회원의 프로필을 조회하면 예외가 발생한다")
    void throwsWhenProfileMemberNotFound() {
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getProfile(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("프로필 수정은 전달한 필드만 변경한다")
    void updatesOnlyProvidedProfileFields() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(savedMember()));

        MemberProfileResponse response = memberService.updateProfile(
                1L, new UpdateProfileRequest(null, "혼밥 전문", null));

        assertThat(response.nickname()).isEqualTo("픽잇러");
        assertThat(response.bio()).isEqualTo("혼밥 전문");
        assertThat(response.profileImageUrl()).isNull();
    }
}
