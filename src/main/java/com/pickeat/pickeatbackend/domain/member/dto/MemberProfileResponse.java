package com.pickeat.pickeatbackend.domain.member.dto;

import com.pickeat.pickeatbackend.domain.member.entity.Member;

public record MemberProfileResponse(
        Long memberId,
        String email,
        String nickname,
        String bio,
        String profileImageUrl
) {

    public static MemberProfileResponse from(Member member) {
        return new MemberProfileResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getBio(),
                member.getProfileImageUrl()
        );
    }
}
