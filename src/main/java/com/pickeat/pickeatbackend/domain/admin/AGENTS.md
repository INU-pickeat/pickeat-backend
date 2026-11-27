# admin 도메인

이 폴더 코드를 고칠 때 지킬 규칙이다. 자세한 결정과 기록은 [docs/domains/admin.md](../../../../../../../../docs/domains/admin.md), 공통 규칙은 루트 [AGENTS.md](../../../../../../../../AGENTS.md).

- 관리자 경로(`/admin.html`, `/api/admin/**`)는 회원 JWT와 별개인 HTTP Basic(`ADMIN_USERNAME`/`ADMIN_PASSWORD`)으로 보호한다. 비밀번호가 비면 기동 실패.
- 집계는 기존 테이블만 읽는다. 이벤트 로그 테이블은 만들지 않는다.
- 운영 상태(`/api/admin/system`)는 앱 안의 Micrometer 지표, `RecentErrors`, `pg_stat_statements`만 읽는다. 앞의 둘은 재시작하면 0부터 센다. 느린 쿼리는 확장이 꺼져 있어도 나머지 응답을 막지 않는다. Google 단가를 바꾸면 `AdminSystemService.GOOGLE_SKUS`도 고친다.
