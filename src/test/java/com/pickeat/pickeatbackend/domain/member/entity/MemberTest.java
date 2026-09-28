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

    @Test
    @DisplayName("프로필 수정 시 빈 문자열은 자기소개와 프로필 이미지를 삭제한다")
    void clearsBioAndProfileImageWithEmptyString() {
        Member member = Member.builder()
                .email("test@pickeat.com")
                .password("encoded-password")
                .nickname("픽잇러")
                .build();
        member.updateProfile(" 새닉네임 ", "소개", "https://cdn.pickeat.com/p.jpg");

        member.updateProfile(null, "", "");

        assertThat(member.getNickname()).isEqualTo("새닉네임");
        assertThat(member.getBio()).isNull();
        assertThat(member.getProfileImageUrl()).isNull();
    }
}
