package com.pickeat.pickeatbackend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements BaseErrorCode {

    INVALID_INPUT("GLOBAL_001", "요청 값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("GLOBAL_002", "로그인이 필요합니다.", HttpStatus.UNAUTHORIZED),
    NOT_FOUND("GLOBAL_003", "요청한 경로를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("GLOBAL_004", "지원하지 않는 HTTP 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),
    INTERNAL_ERROR("GLOBAL_500", "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
