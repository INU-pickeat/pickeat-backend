package com.pickeat.pickeatbackend.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.pickeat.pickeatbackend.domain.user.dto.LoginRequest;
import com.pickeat.pickeatbackend.domain.user.dto.LoginResponse;
import com.pickeat.pickeatbackend.domain.user.dto.SignUpRequest;
import com.pickeat.pickeatbackend.domain.user.entity.Gender;
import com.pickeat.pickeatbackend.domain.user.entity.Job;
import com.pickeat.pickeatbackend.domain.user.entity.User;
import com.pickeat.pickeatbackend.domain.user.repository.UserRepository;
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserService userService;

    private SignUpRequest request() {
        return new SignUpRequest("test@pickeat.com", "password123", Gender.FEMALE, 24, Job.UNIVERSITY_STUDENT);
    }

    private User savedUser() {
        return User.builder()
                .email("test@pickeat.com")
                .password("encoded-password")
                .gender(Gender.FEMALE)
                .age(24)
                .job(Job.UNIVERSITY_STUDENT)
                .build();
    }

    @Test
    void 이미_가입된_이메일이면_예외가_발생한다() {
        when(userRepository.existsByEmail("test@pickeat.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.signUp(request()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 회원가입에_성공하면_비밀번호는_암호화되어_저장된다() {
        when(userRepository.existsByEmail("test@pickeat.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.signUp(request());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void 존재하지_않는_이메일로_로그인하면_예외가_발생한다() {
        when(userRepository.findByEmail("test@pickeat.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(new LoginRequest("test@pickeat.com", "password123")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 비밀번호가_틀리면_예외가_발생한다() {
        when(userRepository.findByEmail("test@pickeat.com")).thenReturn(Optional.of(savedUser()));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(new LoginRequest("test@pickeat.com", "wrong-password")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 로그인에_성공하면_액세스_토큰을_반환한다() {
        when(userRepository.findByEmail("test@pickeat.com")).thenReturn(Optional.of(savedUser()));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(any())).thenReturn("access-token");

        LoginResponse response = userService.login(new LoginRequest("test@pickeat.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
    }
}
