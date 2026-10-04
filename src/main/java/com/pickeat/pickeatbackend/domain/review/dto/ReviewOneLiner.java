package com.pickeat.pickeatbackend.domain.review.dto;

// 한줄평은 앱 내 사용자 후기에서만 만든다. 후기가 없으면 NO_REVIEW를 그대로 내려준다.
public final class ReviewOneLiner {

    public static final String NO_REVIEW = "후기가 없습니다.";
    static final int MAX_LENGTH = 50;

    private ReviewOneLiner() {
    }

    // 후기 본문의 첫 줄을 한줄평으로 쓴다. 50자를 넘으면 잘라서 말줄임표를 붙인다.
    public static String from(String content) {
        if (content == null || content.isBlank()) {
            return NO_REVIEW;
        }
        String firstLine = content.strip().lines().findFirst().orElse("").strip();
        if (firstLine.length() <= MAX_LENGTH) {
            return firstLine;
        }
        return firstLine.substring(0, MAX_LENGTH).stripTrailing() + "…";
    }
}
