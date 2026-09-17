package com.pickeat.pickeatbackend.global.exception;

import org.springframework.http.HttpStatus;

public interface BaseErrorCode {

    String getCode();

    String getMessage();

    HttpStatus getStatus();
}
