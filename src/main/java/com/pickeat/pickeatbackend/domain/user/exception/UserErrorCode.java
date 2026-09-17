package com.pickeat.pickeatbackend.domain.user.exception;

import com.pickeat.pickeatbackend.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    DUPLICATE_EMAIL("USER_001", "이미 가입된 이메일입니다.", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("USER_002", "이메일 또는 비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND("USER_003", "존재하지 않는 회원입니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
