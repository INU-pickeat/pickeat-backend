package com.pickeat.pickeatbackend.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.pickeat.pickeatbackend.domain.user.dto.SignUpRequest;
import com.pickeat.pickeatbackend.domain.user.entity.Gender;
import com.pickeat.pickeatbackend.domain.user.entity.Job;
import com.pickeat.pickeatbackend.domain.user.entity.User;
import com.pickeat.pickeatbackend.domain.user.repository.UserRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
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

    @InjectMocks
    private UserService userService;

    private SignUpRequest request() {
        return new SignUpRequest("test@pickeat.com", "password123", Gender.FEMALE, 24, Job.UNIVERSITY_STUDENT);
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
}
