package com.pickeat.pickeatbackend.domain.review.storage;

import java.time.Instant;

public interface ReviewImageStorage {

    // 클라이언트가 이미지를 직접 올릴 수 있는 임시 URL을 만든다.
    PresignedUpload createUpload(Long memberId, String contentType);

    // 프로필 이미지도 같은 버킷의 profiles/{memberId}/ 아래에 같은 방식으로 올린다.
    PresignedUpload createProfileUpload(Long memberId, String contentType);

    // 이 저장소에 해당 사용자가 올린 이미지의 URL인지 확인한다. 남의 이미지나 외부 URL을 후기에 붙이는 것을 막는다.
    boolean isUploadedBy(Long memberId, String imageUrl);

    record PresignedUpload(String uploadUrl, String imageUrl, Instant expiresAt) {
    }
}
