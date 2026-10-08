package com.pickeat.pickeatbackend.domain.member.controller;

import com.pickeat.pickeatbackend.domain.member.dto.EmailVerificationConfirmRequest;
import com.pickeat.pickeatbackend.domain.member.dto.EmailVerificationRequest;
import com.pickeat.pickeatbackend.domain.member.dto.LoginRequest;
import com.pickeat.pickeatbackend.domain.member.dto.LoginResponse;
import com.pickeat.pickeatbackend.domain.member.dto.MemberProfileResponse;
import com.pickeat.pickeatbackend.domain.member.dto.ProfileImageUploadRequest;
import com.pickeat.pickeatbackend.domain.member.dto.ProfileImageUploadResponse;
import com.pickeat.pickeatbackend.domain.member.dto.RefreshTokenRequest;
import com.pickeat.pickeatbackend.domain.member.dto.SignUpRequest;
import com.pickeat.pickeatbackend.domain.member.dto.SignUpResponse;
import com.pickeat.pickeatbackend.domain.member.dto.UpdateProfileRequest;
import com.pickeat.pickeatbackend.domain.member.service.AuthTokenService;
import com.pickeat.pickeatbackend.domain.member.service.EmailVerificationService;
import com.pickeat.pickeatbackend.domain.member.service.MemberService;
import com.pickeat.pickeatbackend.domain.member.service.ProfileImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final AuthTokenService authTokenService;
    private final EmailVerificationService emailVerificationService;
    private final ProfileImageService profileImageService;

    @PostMapping("/auth/email-verifications")
    public ResponseEntity<Void> requestEmailVerification(
            @Valid @RequestBody EmailVerificationRequest request
    ) {
        emailVerificationService.sendCode(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/email-verifications/confirm")
    public ResponseEntity<Void> confirmEmailVerification(
            @Valid @RequestBody EmailVerificationConfirmRequest request
    ) {
        emailVerificationService.confirm(request.email(), request.code());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/signup")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        Long memberId = memberService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new SignUpResponse(memberId));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(memberService.login(request));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authTokenService.refresh(request.refreshToken()));
    }

    // access token이 만료된 뒤에도 로그아웃할 수 있도록 인증 없이 refresh token만 받는다.
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authTokenService.revoke(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MemberProfileResponse> getMyProfile(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(memberService.getProfile(memberId));
    }

    @PatchMapping("/me")
    public ResponseEntity<MemberProfileResponse> updateMyProfile(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(memberService.updateProfile(memberId, request));
    }

    // 프로필 이미지 업로드 URL 발급. uploadUrl로 PUT 업로드한 뒤 imageUrl을 PATCH /me의 profileImageUrl로 보낸다.
    @PostMapping("/me/profile-image/upload-url")
    public ResponseEntity<ProfileImageUploadResponse> createProfileImageUpload(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ProfileImageUploadRequest request
    ) {
        return ResponseEntity.ok(profileImageService.createUpload(request, memberId));
    }
}
