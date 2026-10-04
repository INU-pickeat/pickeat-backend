package com.pickeat.pickeatbackend.domain.review.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pickeat.pickeatbackend.domain.review.exception.ReviewErrorCode;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class S3ReviewImageStorageTest {

    @Test
    @DisplayName("버킷이 설정되지 않으면 업로드 URL 발급을 거부하고 어떤 URL도 본인 이미지로 인정하지 않는다")
    void rejectsEverythingWhenBucketIsNotConfigured() {
        S3ReviewImageStorage storage = new S3ReviewImageStorage("", "ap-northeast-2", "");

        assertThatThrownBy(() -> storage.createUpload(1L, "image/jpeg"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReviewErrorCode.IMAGE_STORAGE_NOT_CONFIGURED.getMessage());
        assertThat(storage.isUploadedBy(1L, "https://example.com/reviews/1/a.jpg")).isFalse();
    }

    @Test
    @DisplayName("JPEG·PNG·WebP가 아닌 형식은 서명 전에 거부한다")
    void rejectsUnsupportedContentType() {
        S3ReviewImageStorage storage = new S3ReviewImageStorage("pickeat-reviews", "ap-northeast-2", "");

        assertThatThrownBy(() -> storage.createUpload(1L, "image/gif"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ReviewErrorCode.UNSUPPORTED_IMAGE_TYPE.getMessage());
        assertThatThrownBy(() -> storage.createUpload(1L, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("본인 경로(reviews/{memberId}/) 아래의 URL만 본인 이미지로 인정한다")
    void acceptsOnlyOwnPrefix() {
        S3ReviewImageStorage storage =
                new S3ReviewImageStorage("pickeat-reviews", "ap-northeast-2", "https://img.pickeat.kr/");

        assertThat(storage.isUploadedBy(1L, "https://img.pickeat.kr/reviews/1/a.jpg")).isTrue();
        assertThat(storage.isUploadedBy(1L, "https://img.pickeat.kr/reviews/2/a.jpg")).isFalse();
        assertThat(storage.isUploadedBy(1L, "https://img.pickeat.kr/reviews/12/a.jpg")).isFalse();
        assertThat(storage.isUploadedBy(1L, "https://evil.example.com/reviews/1/a.jpg")).isFalse();
        assertThat(storage.isUploadedBy(1L, "https://img.pickeat.kr/reviews/1/")).isFalse();
        assertThat(storage.isUploadedBy(1L, null)).isFalse();
    }

    @Test
    @DisplayName("제공 주소를 지정하지 않으면 S3 버킷 주소를 기준으로 삼는다")
    void defaultsPublicBaseUrlToBucketAddress() {
        S3ReviewImageStorage storage = new S3ReviewImageStorage("pickeat-reviews", "ap-northeast-2", "");

        assertThat(storage.isUploadedBy(
                7L, "https://pickeat-reviews.s3.ap-northeast-2.amazonaws.com/reviews/7/a.png")).isTrue();
    }
}
