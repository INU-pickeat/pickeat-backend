package com.pickeat.pickeatbackend.domain.member.service;

import com.pickeat.pickeatbackend.domain.member.dto.LoginRequest;
import com.pickeat.pickeatbackend.domain.member.dto.LoginResponse;
import com.pickeat.pickeatbackend.domain.member.dto.MemberProfileResponse;
import com.pickeat.pickeatbackend.domain.member.dto.PasswordResetRequest;
import com.pickeat.pickeatbackend.domain.member.dto.SignUpRequest;
import com.pickeat.pickeatbackend.domain.member.dto.UpdateProfileRequest;
import com.pickeat.pickeatbackend.domain.member.entity.Member;
import com.pickeat.pickeatbackend.domain.member.exception.MemberErrorCode;
import com.pickeat.pickeatbackend.domain.member.repository.MemberRepository;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public Long signUp(SignUpRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(MemberErrorCode.DUPLICATE_EMAIL);
        }
        emailVerificationService.consume(request.email());

        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build();

        return memberRepository.save(member).getId();
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(MemberErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(MemberErrorCode.INVALID_CREDENTIALS);
        }

        return authTokenService.issue(member);
    }

    // 인증을 마친 이메일의 비밀번호를 바꾸고, 모든 기기의 refresh token을 폐기해 다시 로그인하게 한다.
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
        emailVerificationService.consume(request.email());
        member.changePassword(passwordEncoder.encode(request.newPassword()));
        authTokenService.revokeAll(member.getId());
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
