CREATE TABLE questions (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    survey_id       BIGINT NOT NULL REFERENCES surveys(id),
    question_order  INT NOT NULL,
    type            VARCHAR(20) NOT NULL,
    content         VARCHAR(200) NOT NULL,
    is_required     BOOLEAN NOT NULL DEFAULT FALSE,
    allow_multiple  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL
);
