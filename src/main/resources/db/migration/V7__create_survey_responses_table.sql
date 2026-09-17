CREATE TABLE survey_responses (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    survey_id      BIGINT NOT NULL REFERENCES surveys(id),
    respondent_id  BIGINT REFERENCES users(id),
    guest_key      VARCHAR(64),
    earned_token   INT NOT NULL DEFAULT 0,
    submitted_at   TIMESTAMP NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    UNIQUE (survey_id, respondent_id),
    UNIQUE (survey_id, guest_key)
);
