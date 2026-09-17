CREATE TABLE surveys (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    creator_id         BIGINT NOT NULL REFERENCES users(id),
    title              VARCHAR(100) NOT NULL,
    description        VARCHAR(1000),
    target             VARCHAR(50),
    category           VARCHAR(20) NOT NULL,
    estimated_minutes  INT,
    start_date         DATE NOT NULL,
    end_date           DATE NOT NULL,
    shared_to_archive  BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at        TIMESTAMP,
    is_deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP NOT NULL
);
