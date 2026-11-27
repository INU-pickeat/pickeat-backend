package com.pickeat.pickeatbackend.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecentErrorsTest {

    @Test
    @DisplayName("최신순으로 최대 50건만 남기고 긴 메시지는 자른다")
    void keepsLatestFiftyNewestFirst() {
        RecentErrors recentErrors = new RecentErrors();
        for (int i = 0; i < 60; i++) {
            recentErrors.record("GET", "/path/" + i, new IllegalStateException("x".repeat(500)));
        }

        assertThat(recentErrors.list()).hasSize(RecentErrors.CAPACITY);
        assertThat(recentErrors.list().getFirst().path()).isEqualTo("/path/59");
        assertThat(recentErrors.list().getLast().path()).isEqualTo("/path/10");
        assertThat(recentErrors.list().getFirst().message()).hasSize(300);
    }
}
