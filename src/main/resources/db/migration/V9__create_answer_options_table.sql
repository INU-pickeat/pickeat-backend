CREATE TABLE answer_options (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    answer_id  BIGINT NOT NULL REFERENCES answers(id),
    option_id  BIGINT NOT NULL REFERENCES question_options(id)
);
