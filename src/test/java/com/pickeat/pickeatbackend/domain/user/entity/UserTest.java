package com.pickeat.pickeatbackend.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void 빌더로_생성하면_토큰잔액은_0으로_초기화된다() {
        User user = User.builder()
                .email("test@pickeat.com")
                .password("encoded-password")
                .gender(Gender.FEMALE)
                .age(24)
                .job(Job.UNIVERSITY_STUDENT)
                .build();

        assertThat(user.getTokenBalance()).isZero();
        assertThat(user.getEmail()).isEqualTo("test@pickeat.com");
    }
}
