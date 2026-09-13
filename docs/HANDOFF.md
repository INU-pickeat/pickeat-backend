# 개발 인계 — 2026-09-11

> 2026-09-13 업데이트: V1과 CI·Swagger 변경은 origin/develop의 PR #3, #4에 반영되어 있다.
> 최신 서비스 결정은 [SERVICE_DECISIONS.md](SERVICE_DECISIONS.md)를 우선한다.
> V2 SQL 및 공간·동행 스키마 테스트를 추가했다. 실제 식당 자료 입력은 아직 진행하지 않았다.

## 완료

- Java 21 / Spring Boot 4.1.1 빌드 확인.
- 로컬 PostgreSQL 18.6, PostGIS 3.6.3 연결 확인.
- Flyway V1로 restaurants 생성. 실제 식당 자료는 아직 없음.
- Testcontainers PostgreSQL 18.6으로 테스트 DB 격리 및 Flyway 검증.
- 로컬 Swagger 공개, 기본 환경 문서 비활성화, 업무 경로 인증 유지.
- 테스트 5개 로컬 통과. CI와 CodeRabbit 설정 파일 추가.

## 아직 미구현

- Restaurant Entity / Repository / 업무 API.
- PostGIS 공간 쿼리·인덱스·통합 테스트.
- CodeRabbit GitHub App 연결 확인 및 첫 리뷰 확인.
- 실제 사용자 인증, CD, 모니터링 수집·대시보드·알림, S3 연결.

## 다음 작업

1. PR의 GitHub Actions 결과 확인. 실패 시 먼저 해결하고 병합은 사용자 확인 후 진행.
2. 식당 자료의 사진·소개·출처 연결을 포함한 최소 ERD 합의.
3. Restaurant Entity / Repository 및 저장·조회 테스트 구현.
4. 중심 좌표와 추천 기준 확정 후 반경 검색·상세 API 구현.

## 유지할 서비스 결정

- 기존 분위기 태그 8개와 자동 태그 추출은 사용하지 않는다.
- 입력은 누구와 / 어디서 / 어떤 음식 중심. 동행 적합 판단 기준은 미확정.
- 전시 중심 좌표 기준 5km 내 식당 자료를 팀이 제공하고 DB 형식으로 정리한다.
- 팀이 작성한 블로그 원문·사진 활용을 검토한다. 외부 저작물과 구분한다.
- 추천 후보가 0개면 없음, 1~4개면 그대로, 5개 이상이면 최대 5개.
- 리뷰 경험 카테고리와 통계는 추후 구현하며, 현재 추천 입력과 혼동하지 않는다.
- Redis·과도한 미래 추상화는 도입하지 않는다.

## 주의

- 이미 로컬에 적용된 V1은 수정하지 않고 다음 마이그레이션을 추가한다.
- 테스트는 Docker가 필요하다. IntelliJ 테스트 실행은 Gradle을 사용한다.
- 기존 개발 DB를 테스트용으로 사용하지 않는다.
- 배포 환경에 local 프로필을 사용하지 않는다.
- main 직접 push 금지. 작업 브랜치와 PR을 사용한다.
