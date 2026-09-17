package com.pickeat.pickeatbackend.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pickeat.pickeatbackend.domain.member.dto.LoginRequest;
import com.pickeat.pickeatbackend.domain.member.dto.LoginResponse;
import com.pickeat.pickeatbackend.domain.member.dto.SignUpRequest;
import com.pickeat.pickeatbackend.domain.member.entity.Member;
import com.pickeat.pickeatbackend.domain.member.repository.MemberRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import com.pickeat.pickeatbackend.global.security.jwt.JwtTokenProvider;
import java.util.Optional;
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
    private JwtTokenProvider jwtTokenProvider;

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
    void 이미_가입된_이메일이면_예외가_발생한다() {
        when(memberRepository.existsByEmail("test@pickeat.com")).thenReturn(true);

        assertThatThrownBy(() -> memberService.signUp(request()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 회원가입에_성공하면_비밀번호는_암호화되어_저장된다() {
        when(memberRepository.existsByEmail("test@pickeat.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        memberService.signUp(request());

        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void 존재하지_않는_이메일로_로그인하면_예외가_발생한다() {
        when(memberRepository.findByEmail("test@pickeat.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.login(new LoginRequest("test@pickeat.com", "password123")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 비밀번호가_틀리면_예외가_발생한다() {
        when(memberRepository.findByEmail("test@pickeat.com")).thenReturn(Optional.of(savedMember()));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> memberService.login(new LoginRequest("test@pickeat.com", "wrong-password")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 로그인에_성공하면_액세스_토큰을_반환한다() {
        when(memberRepository.findByEmail("test@pickeat.com")).thenReturn(Optional.of(savedMember()));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(any())).thenReturn("access-token");

        LoginResponse response = memberService.login(new LoginRequest("test@pickeat.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
    }
}