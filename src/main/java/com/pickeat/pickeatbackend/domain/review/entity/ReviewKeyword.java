package com.pickeat.pickeatbackend.domain.review.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 후기 작성 시 고르는 "이런 점이 좋았어요" 키워드. 선언 순서가 응답 정렬 순서다(맛 → 매장 → 서비스 → 동행).
@Getter
@RequiredArgsConstructor
public enum ReviewKeyword {
    TASTY("음식이 맛있어요"),
    FRESH_INGREDIENTS("재료가 신선해요"),
    GENEROUS_PORTION("양이 많아요"),
    SIGNATURE_MENU("특별한 메뉴가 있어요"),
    GOOD_VALUE("가성비가 좋아요"),
    NICE_ATMOSPHERE("분위기가 좋아요"),
    CLEAN("매장이 청결해요"),
    COMFORTABLE_SEATING("좌석이 편해요"),
    GOOD_FOR_TALKING("대화하기 좋아요"),
    KIND_SERVICE("친절해요"),
    QUICK_SERVING("음식이 빨리 나와요"),
    GOOD_FOR_DATE("데이트하기 좋아요"),
    GOOD_FOR_SOLO("혼밥하기 편해요"),
    GOOD_FOR_GROUP("단체로 가기 좋아요"),
    GOOD_FOR_CHILDREN("아이와 가기 좋아요"),
    GOOD_FOR_FAMILY("가족 모임에 좋아요"),
    DOG_FRIENDLY("반려견과 함께할 수 있어요");

    private final String label;
}
