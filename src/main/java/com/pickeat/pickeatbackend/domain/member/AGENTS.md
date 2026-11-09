# member 도메인

이 폴더 코드를 고칠 때 지킬 규칙이다. 자세한 결정과 기록은 [docs/domains/member.md](../../../../../../../../docs/domains/member.md), 공통 규칙은 루트 [AGENTS.md](../../../../../../../../AGENTS.md).

- 인증 API(가입·로그인·이메일 인증·비밀번호 찾기·토큰 재발급·로그아웃)도 이 패키지에 있다.
- access token은 JWT 30분, refresh token은 무작위 값 14일이며 DB에는 해시만 저장하고 재발급 때마다 교체(rotation)한다.
- 이메일은 항상 `strip()` + 소문자로 정규화한다. 인증번호는 DB에 BCrypt 해시만 저장한다.
- 비밀번호를 바꾸면 그 회원의 refresh token을 모두 지운다.
- 닉네임은 한글 완성형·영문·숫자 최대 10자.
