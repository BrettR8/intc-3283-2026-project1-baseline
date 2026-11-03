package edu.northwestu.project1p2.entities;

import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A row in {@code email_verification_tokens}. Holds the SHA-256 hash of a
 * one-time confirmation token, never the token itself.
 */
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {

    @Id
    private @Nullable Long id;

    private Long userId;

    private String tokenHash;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    /** Set when the token is consumed or superseded; {@code null} while usable. */
    private @Nullable LocalDateTime usedAt;

    public EmailVerificationToken(final Long userId, final String tokenHash,
                                  final LocalDateTime createdAt, final LocalDateTime expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    /** True when the token has not been used and has not expired at {@code now}. */
    public boolean isUsableAt(final LocalDateTime now) {
        return this.usedAt == null && now.isBefore(this.expiresAt);
    }

    public @Nullable Long getId() {
        return this.id;
    }

    public Long getUserId() {
        return this.userId;
    }

    public String getTokenHash() {
        return this.tokenHash;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return this.expiresAt;
    }

    public @Nullable LocalDateTime getUsedAt() {
        return this.usedAt;
    }
}
