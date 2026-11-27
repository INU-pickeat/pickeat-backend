package com.pickeat.pickeatbackend.global.exception;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 처리하지 못한 예외(500) 최근 {@value #CAPACITY}건. 관리자 운영 상태 화면에서 본다.
 * ponytail: 서버 메모리에만 있어 재시작하면 비워진다. 기간별 기록·알림이 필요하면 Sentry 같은 외부 도구로 옮긴다.
 */
@Component
public class RecentErrors {

    static final int CAPACITY = 50;
    private static final int MAX_MESSAGE_LENGTH = 300;

    private final Deque<Entry> entries = new ArrayDeque<>(CAPACITY);

    public synchronized void record(String method, String path, Throwable error) {
        if (entries.size() == CAPACITY) {
            entries.removeLast();
        }
        String message = error.getMessage();
        if (message != null && message.length() > MAX_MESSAGE_LENGTH) {
            message = message.substring(0, MAX_MESSAGE_LENGTH);
        }
        entries.addFirst(new Entry(Instant.now(), method, path, error.getClass().getName(), message));
    }

    /** 최신순. */
    public synchronized List<Entry> list() {
        return List.copyOf(entries);
    }

    public record Entry(Instant occurredAt, String method, String path, String type, String message) {
    }
}
