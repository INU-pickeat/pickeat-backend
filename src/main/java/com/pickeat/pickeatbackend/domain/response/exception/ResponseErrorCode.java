package com.pickeat.pickeatbackend.domain.response.exception;

import com.pickeat.pickeatbackend.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ResponseErrorCode implements BaseErrorCode {

    CANNOT_RESPOND_OWN_SURVEY("RESPONSE_001", "본인 설문에는 참여할 수 없습니다.", HttpStatus.FORBIDDEN),
    DUPLICATE_RESPONSE("RESPONSE_002", "이미 참여한 설문입니다.", HttpStatus.CONFLICT),
    GUEST_KEY_REQUIRED("RESPONSE_003", "게스트 참여에는 guestKey가 필요합니다.", HttpStatus.BAD_REQUEST),
    REQUIRED_QUESTION_NOT_ANSWERED("RESPONSE_004", "필수 문항에 답변하지 않았습니다.", HttpStatus.BAD_REQUEST),
    INVALID_ANSWER("RESPONSE_005", "답변 형식이 올바르지 않습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
