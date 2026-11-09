# pick 도메인

이 폴더 코드를 고칠 때 지킬 규칙이다. 자세한 결정과 기록은 [docs/domains/pick.md](../../../../../../../../docs/domains/pick.md), 공통 규칙은 루트 [AGENTS.md](../../../../../../../../AGENTS.md).

- 추천 세션당 Pick은 하나. 같은 식당은 다른 세션에서 다시 Pick할 수 있다.
- 상태는 `SELECTED`→`REVIEWED`/`CANCELED`만 전이한다. `REVIEWED`는 후기 작성으로만 되고, 후기를 지우면 `SELECTED`로 돌아간다.
- Pick 대상은 본인 추천 세션의 후보 식당이어야 한다. 다른 회원의 세션·Pick은 404로 숨긴다.
- 지도·캘린더는 `REVIEWED` Pick만 보여준다. 날짜 경계는 한국 시간 기준.
