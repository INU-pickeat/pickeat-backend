# Quality Gates

PR을 올리기 전에 확인하는 기준이다. 각 항목의 상세 규칙은 링크된 문서를 따른다.

- [ ] `./gradlew test`가 통과한다 (CI도 PostGIS 서비스로 `./gradlew build`를 돌린다)
- [ ] PR 제목이 커밋 메시지 형식이고 설명은 한국어다 ([development-cycle.md](development-cycle.md#커밋-메시지))
- [ ] 하나의 브랜치·PR에 하나의 작업 목적만 있다
- [ ] Entity 변경과 마이그레이션 SQL이 같은 PR에 있고, 이미 실행된 마이그레이션 파일은 고치지 않았다 ([persistence.md](persistence.md#마이그레이션))
- [ ] Entity를 API 요청·응답으로 직접 노출하지 않았다 ([layer-boundaries.md](layer-boundaries.md#dto))
- [ ] 새 오류는 도메인 ErrorCode에 번호를 이어 붙였다 ([api-conventions.md](api-conventions.md#오류))
- [ ] 비밀번호, 토큰, 인증번호 원문을 로그·응답에 남기지 않았다
- [ ] `.env`, API 키, 비밀번호가 커밋에 없다. 새 비밀값은 환경변수로 받고 `docs/DEPLOYMENT.md`에 적었다
- [ ] API 계약이 바뀌었으면 README "현재 구현 범위"와 Notion API SPECIFICATION을 갱신했다
- [ ] 기존 요청에 **필수 필드를 추가하지 않았다.** 필요하면 선택 필드로 먼저 배포하고, 프론트가 배포된 뒤 필수로 바꾼다(2026-11-03 후기 작성 장애, `progress.md` "운영 장애 기록")
- [ ] `docs/progress.md` 구현 현황을 갱신했다. 결정이 바뀌었으면 `SERVICE_DECISIONS.md`를 고치고 `history.md`에 경위를 남겼다
- [ ] DB 마이그레이션·인증·동시성 변경은 작성자가 아닌 에이전트나 사람에게 리뷰를 받았다 ([workflows/orchestration.md](workflows/orchestration.md))
