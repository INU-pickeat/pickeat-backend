package com.pickeat.pickeatbackend.domain.member.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    @DisplayName("빌더로 생성하면 로그인 제공자는 LOCAL로 초기화된다")
    void defaultsToLocalLoginProviderWhenBuilt() {
        Member member = Member.builder()
                .email("test@pickeat.com")
                .password("encoded-password")
                .nickname("픽잇러")
                .build();

        assertThat(member.getLoginProvider()).isEqualTo(LoginProvider.LOCAL);
        assertThat(member.getNickname()).isEqualTo("픽잇러");
    }
}
