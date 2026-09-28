CREATE TABLE refresh_tokens (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id   BIGINT NOT NULL REFERENCES members (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT refresh_tokens_token_hash_unique UNIQUE (token_hash)
);

CREATE INDEX refresh_tokens_member_id_idx ON refresh_tokens (member_id);

COMMENT ON TABLE refresh_tokens IS '로그인 기기별 refresh token. 재발급할 때마다 기존 행을 지우고 새 행을 만든다(rotation)';
COMMENT ON COLUMN refresh_tokens.token_hash IS '원문 토큰의 SHA-256 hex. 원문은 저장하지 않는다';
