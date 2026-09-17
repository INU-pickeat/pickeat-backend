package com.pickeat.pickeatbackend.domain.member.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    void 빌더로_생성하면_로그인_제공자는_LOCAL로_초기화된다() {
        Member member = Member.builder()
                .email("test@pickeat.com")
                .password("encoded-password")
                .nickname("픽잇러")
                .build();

        assertThat(member.getLoginProvider()).isEqualTo(LoginProvider.LOCAL);
        assertThat(member.getNickname()).isEqualTo("픽잇러");
    }
}