DROP TABLE IF EXISTS users;

CREATE TABLE members (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email              VARCHAR(255) NOT NULL UNIQUE,
    password           VARCHAR(255) NOT NULL,
    nickname           VARCHAR(30) NOT NULL,
    login_provider     VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    profile_image_url  VARCHAR(500),
    default_region     VARCHAR(100),
    created_at         TIMESTAMP NOT NULL,
    updated_at         TIMESTAMP NOT NULL
);