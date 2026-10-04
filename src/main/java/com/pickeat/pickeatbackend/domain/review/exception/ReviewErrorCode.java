package com.pickeat.pickeatbackend.domain.review.exception;

import com.pickeat.pickeatbackend.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements BaseErrorCode {
    REVIEW_NOT_FOUND("REVIEW_001", "존재하지 않는 후기입니다.", HttpStatus.NOT_FOUND),
    REVIEW_ALREADY_EXISTS("REVIEW_002", "이미 후기가 작성된 Pick입니다.", HttpStatus.CONFLICT),
    REVIEW_NOT_PUBLIC("REVIEW_003", "비공개 후기에는 좋아요를 누를 수 없습니다.", HttpStatus.CONFLICT),
    INVALID_IMAGE_URL("REVIEW_004", "업로드 URL로 직접 올린 이미지만 첨부할 수 있습니다.", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_IMAGE_TYPE("REVIEW_005", "JPEG, PNG, WebP 이미지만 올릴 수 있습니다.", HttpStatus.BAD_REQUEST),
    IMAGE_STORAGE_NOT_CONFIGURED("REVIEW_006", "이미지 저장소가 아직 준비되지 않았습니다.", HttpStatus.SERVICE_UNAVAILABLE);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
