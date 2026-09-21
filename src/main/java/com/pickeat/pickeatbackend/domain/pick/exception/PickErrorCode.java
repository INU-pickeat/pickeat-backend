package com.pickeat.pickeatbackend.domain.pick.exception;

import com.pickeat.pickeatbackend.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PickErrorCode implements BaseErrorCode {

    PICK_NOT_FOUND("PICK_001", "존재하지 않는 Pick입니다.", HttpStatus.NOT_FOUND),
    RESTAURANT_NOT_IN_SESSION("PICK_002", "추천 결과에 포함되지 않은 식당입니다.", HttpStatus.BAD_REQUEST),
    SESSION_ALREADY_PICKED("PICK_003", "이미 최종 선택이 기록된 추천 세션입니다.", HttpStatus.CONFLICT),
    INVALID_STATUS_TRANSITION("PICK_004", "허용되지 않는 Pick 상태 변경입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
