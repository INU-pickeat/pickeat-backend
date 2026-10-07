package com.pickeat.pickeatbackend.domain.member.exception;

import com.pickeat.pickeatbackend.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {

    DUPLICATE_EMAIL("MEMBER_001", "이미 가입된 이메일입니다.", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("MEMBER_002", "이메일 또는 비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    MEMBER_NOT_FOUND("MEMBER_003", "회원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_REFRESH_TOKEN("MEMBER_004", "로그인이 만료되었습니다. 다시 로그인해 주세요.", HttpStatus.UNAUTHORIZED),
    EMAIL_VERIFICATION_REQUIRED("MEMBER_005", "이메일 인증이 필요합니다.", HttpStatus.FORBIDDEN),
    INVALID_EMAIL_VERIFICATION_CODE("MEMBER_006", "이메일 인증번호가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    EXPIRED_EMAIL_VERIFICATION_CODE("MEMBER_007", "이메일 인증번호가 만료되었습니다.", HttpStatus.GONE),
    EMAIL_VERIFICATION_TOO_FREQUENT("MEMBER_008", "인증번호는 1분 후 다시 요청할 수 있습니다.", HttpStatus.TOO_MANY_REQUESTS),
    EMAIL_DELIVERY_FAILED("MEMBER_009", "인증 메일을 보내지 못했습니다.", HttpStatus.SERVICE_UNAVAILABLE);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
