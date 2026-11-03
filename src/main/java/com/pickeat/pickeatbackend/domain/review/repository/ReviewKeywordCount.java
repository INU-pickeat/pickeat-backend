package com.pickeat.pickeatbackend.domain.review.repository;

import com.pickeat.pickeatbackend.domain.review.entity.ReviewKeyword;

public record ReviewKeywordCount(ReviewKeyword keyword, Long count) {
}
