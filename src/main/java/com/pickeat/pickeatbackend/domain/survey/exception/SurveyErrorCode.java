package com.pickeat.pickeatbackend.domain.survey.exception;

import com.pickeat.pickeatbackend.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SurveyErrorCode implements BaseErrorCode {

    INVALID_DATE_RANGE("SURVEY_001", "마감일은 시작일보다 빠를 수 없습니다.", HttpStatus.BAD_REQUEST),
    SURVEY_NOT_FOUND("SURVEY_002", "존재하지 않는 설문입니다.", HttpStatus.NOT_FOUND),
    OPTIONS_REQUIRED("SURVEY_003", "객관식 문항에는 보기가 필요합니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
