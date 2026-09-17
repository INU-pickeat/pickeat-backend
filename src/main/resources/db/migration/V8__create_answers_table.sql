CREATE TABLE answers (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    response_id  BIGINT NOT NULL REFERENCES survey_responses(id),
    question_id  BIGINT NOT NULL REFERENCES questions(id),
    answer_text  TEXT,
    created_at   TIMESTAMP NOT NULL
);
