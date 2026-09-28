package com.pickeat.pickeatbackend.domain.pick.dto;

import com.pickeat.pickeatbackend.global.exception.BusinessException;
import com.pickeat.pickeatbackend.global.exception.GlobalErrorCode;
import java.time.Duration;

public enum PickPeriod {
    WEEK(Duration.ofDays(7)),
    MONTH(Duration.ofDays(30));

    private final Duration window;

    PickPeriod(Duration window) {
        this.window = window;
    }

    public Duration window() {
        return window;
    }

    public static PickPeriod from(String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT);
        }
    }
}
