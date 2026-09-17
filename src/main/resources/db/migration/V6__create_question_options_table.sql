CREATE TABLE question_options (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_id   BIGINT NOT NULL REFERENCES questions(id),
    option_order  INT NOT NULL,
    content       VARCHAR(200) NOT NULL,
    created_at    TIMESTAMP NOT NULL
);
