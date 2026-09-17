package com.pickeat.pickeatbackend.global.exception;

public record ErrorResponse(String code, String message) {

    public static ErrorResponse from(BaseErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }
}
