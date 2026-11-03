package com.pickeat.pickeatbackend.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SecurityConfigTest {

    @Test
    @DisplayName("관리자 비밀번호가 비어 있거나 공백뿐이면 기동을 막는다")
    void rejectsBlankAdminPassword() {
        assertThatThrownBy(() -> SecurityConfig.requireAdminPassword(""))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> SecurityConfig.requireAdminPassword("   "))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> SecurityConfig.requireAdminPassword(null))
                .isInstanceOf(IllegalStateException.class);
        assertThat(SecurityConfig.requireAdminPassword("s3cret")).isEqualTo("s3cret");
    }
}
