-- One-time email confirmation tokens. Only a SHA-256 hash of the token is stored,
-- so a leaked database row cannot be turned back into a working link.
CREATE TABLE email_verification_tokens
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash TEXT      NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used_at    TIMESTAMP NULL
);

CREATE INDEX email_verification_tokens_user_id_idx ON email_verification_tokens (user_id);
