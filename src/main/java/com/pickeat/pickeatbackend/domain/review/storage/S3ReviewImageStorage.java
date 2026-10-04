package com.pickeat.pickeatbackend.domain.review.storage;

import com.pickeat.pickeatbackend.domain.review.exception.ReviewErrorCode;
import com.pickeat.pickeatbackend.global.exception.BusinessException;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

// 후기 이미지는 클라이언트가 S3 presigned URL로 직접 올리고, 서버는 URL만 저장한다.
// 버킷이 설정되지 않은 환경(로컬·테스트, 버킷 생성 전 운영)에서도 애플리케이션은 정상 기동하며,
// 업로드 URL 발급만 REVIEW_006으로 거부한다.
@Component
public class S3ReviewImageStorage implements ReviewImageStorage {

    private static final Duration UPLOAD_URL_VALIDITY = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "reviews/";
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final String bucket;
    private final String region;
    private final String publicBaseUrl;
    private S3Presigner presigner;

    public S3ReviewImageStorage(
            @Value("${storage.s3.bucket:}") String bucket,
            @Value("${storage.s3.region:ap-northeast-2}") String region,
            @Value("${storage.public-base-url:}") String publicBaseUrl
    ) {
        this.bucket = bucket == null ? "" : bucket.strip();
        this.region = region == null || region.isBlank() ? "ap-northeast-2" : region.strip();
        this.publicBaseUrl = resolvePublicBaseUrl(publicBaseUrl, this.bucket, this.region);
    }

    @Override
    public PresignedUpload createUpload(Long memberId, String contentType) {
        if (bucket.isEmpty()) {
            throw new BusinessException(ReviewErrorCode.IMAGE_STORAGE_NOT_CONFIGURED);
        }
        String normalizedContentType = contentType == null ? "" : contentType.strip().toLowerCase(Locale.ROOT);
        String extension = EXTENSIONS.get(normalizedContentType);
        if (extension == null) {
            throw new BusinessException(ReviewErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }
        String key = KEY_PREFIX + memberId + "/" + UUID.randomUUID() + "." + extension;
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(normalizedContentType)
                .build();
        PresignedPutObjectRequest presigned = presigner().presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(UPLOAD_URL_VALIDITY)
                .putObjectRequest(putObjectRequest)
                .build());
        return new PresignedUpload(presigned.url().toString(), publicBaseUrl + "/" + key, presigned.expiration());
    }

    @Override
    public boolean isUploadedBy(Long memberId, String imageUrl) {
        if (bucket.isEmpty() || imageUrl == null) {
            return false;
        }
        String ownPrefix = publicBaseUrl + "/" + KEY_PREFIX + memberId + "/";
        return imageUrl.startsWith(ownPrefix) && imageUrl.length() > ownPrefix.length();
    }

    // 자격 증명은 EC2 인스턴스 역할 등 기본 체인에서 실제 서명 시점에 읽는다. 기동 시에는 AWS에 접근하지 않는다.
    private synchronized S3Presigner presigner() {
        if (presigner == null) {
            presigner = S3Presigner.builder().region(Region.of(region)).build();
        }
        return presigner;
    }

    @PreDestroy
    synchronized void close() {
        if (presigner != null) {
            presigner.close();
        }
    }

    // CloudFront 도메인이 주어지면 그 주소로, 없으면 S3 가상 호스트 주소로 이미지를 제공한다.
    private static String resolvePublicBaseUrl(String configured, String bucket, String region) {
        if (configured != null && !configured.isBlank()) {
            String trimmed = configured.strip();
            return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        }
        return bucket.isEmpty() ? "" : "https://" + bucket + ".s3." + region + ".amazonaws.com";
    }
}
