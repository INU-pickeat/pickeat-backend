# review 도메인

이 폴더 코드를 고칠 때 지킬 규칙이다. 자세한 결정과 기록은 [docs/domains/review.md](../../../../../../../../docs/domains/review.md), 공통 규칙은 루트 [AGENTS.md](../../../../../../../../AGENTS.md).

- 후기는 Pick 하나당 하나(`REVIEW_002`). 사진 1~5장 필수(`REVIEW_008`), 사진은 수정 불가(`REVIEW_007`).
- 키워드 필수 여부는 `ReviewService.KEYWORDS_REQUIRED`(프론트 배포 전까지 임시 false).
- 이미지는 S3 presigned URL로 클라이언트가 직접 올리고, 후기에는 본인 경로 `reviews/{memberId}/` URL만 붙인다.
- 공개 피드·좋아요·식당 요약은 `PUBLIC` 후기만. 남의 비공개 후기는 존재를 숨긴다(404).
- 요청에 필수 필드를 추가할 때는 선택으로 먼저 배포하고 프론트 배포 후 필수로 바꾼다(2026-11-03 장애, [progress.md](../../../../../../../../docs/progress.md) "운영 장애 기록").
