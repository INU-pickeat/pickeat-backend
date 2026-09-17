package com.pickeat.pickeatbackend.domain.member.controller;

import com.pickeat.pickeatbackend.domain.member.dto.LoginRequest;
import com.pickeat.pickeatbackend.domain.member.dto.LoginResponse;
import com.pickeat.pickeatbackend.domain.member.dto.SignUpRequest;
import com.pickeat.pickeatbackend.domain.member.dto.SignUpResponse;
import com.pickeat.pickeatbackend.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        Long memberId = memberService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new SignUpResponse(memberId));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(memberService.login(request));
    }
}