package com.pickeat.pickeatbackend.domain.member.exception;

import com.pickeat.pickeatbackend.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {

    DUPLICATE_EMAIL("MEMBER_001", "이미 가입된 이메일입니다.", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("MEMBER_002", "이메일 또는 비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED);

    private final String code;
    private final String message;
    private final HttpStatus status;
}