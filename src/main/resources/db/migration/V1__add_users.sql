CREATE TABLE users
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username    TEXT                                NOT NULL,
    email       TEXT UNIQUE                         NOT NULL,
    password    TEXT                                NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    verified_at TIMESTAMP                           NULL
);
