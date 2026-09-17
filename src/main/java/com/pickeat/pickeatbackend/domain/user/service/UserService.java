package com.pickeat.pickeatbackend.domain.user.service;

import com.pickeat.pickeatbackend.domain.user.dto.SignUpRequest;
import com.pickeat.pickeatbackend.domain.user.entity.User;
import com.pickeat.pickeatbackend.domain.user.exception.UserErrorCode;
import com.pickeat.pickeatbackend.domain.user.repository.UserRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .gender(request.gender())
                .age(request.age())
                .job(request.job())
                .build();

        return userRepository.save(user).getId();
    }
}
