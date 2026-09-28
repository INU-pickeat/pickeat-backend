package com.pickeat.pickeatbackend.domain.member.service;

import com.pickeat.pickeatbackend.domain.member.dto.LoginRequest;
import com.pickeat.pickeatbackend.domain.member.dto.LoginResponse;
import com.pickeat.pickeatbackend.domain.member.dto.MemberProfileResponse;
import com.pickeat.pickeatbackend.domain.member.dto.SignUpRequest;
import com.pickeat.pickeatbackend.domain.member.dto.UpdateProfileRequest;
import com.pickeat.pickeatbackend.domain.member.entity.Member;
import com.pickeat.pickeatbackend.domain.member.exception.MemberErrorCode;
import com.pickeat.pickeatbackend.domain.member.repository.MemberRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import com.pickeat.pickeatbackend.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public Long signUp(SignUpRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(MemberErrorCode.DUPLICATE_EMAIL);
        }

        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build();

        return memberRepository.save(member).getId();
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(MemberErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(MemberErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.createAccessToken(member.getId());
        return new LoginResponse(accessToken);
    }

    @Transactional(readOnly = true)
    public MemberProfileResponse getProfile(Long memberId) {
        return MemberProfileResponse.from(findMember(memberId));
    }

    @Transactional
    public MemberProfileResponse updateProfile(Long memberId, UpdateProfileRequest request) {
        Member member = findMember(memberId);
        member.updateProfile(request.nickname(), request.bio(), request.profileImageUrl());
        return MemberProfileResponse.from(member);
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
