package com.pickeat.pickeatbackend.domain.member.service;

import com.pickeat.pickeatbackend.domain.member.dto.ProfileImageUploadRequest;
import com.pickeat.pickeatbackend.domain.member.dto.ProfileImageUploadResponse;
import com.pickeat.pickeatbackend.domain.review.storage.ReviewImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 프로필 이미지는 후기 이미지와 같은 S3 버킷(profiles/{memberId}/)에 클라이언트가 직접 올린다.
// 형식 오류·저장소 미설정 응답도 후기 이미지와 같다(400 REVIEW_005, 503 REVIEW_006).
@Service
@RequiredArgsConstructor
public class ProfileImageService {

    private final ReviewImageStorage imageStorage;

    public ProfileImageUploadResponse createUpload(ProfileImageUploadRequest request, Long memberId) {
        return ProfileImageUploadResponse.of(
                imageStorage.createProfileUpload(memberId, request.contentType()), request.contentType());
    }
}
