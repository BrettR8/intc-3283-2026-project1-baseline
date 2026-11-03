package edu.northwestu.project1p2.entities;

import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A row in {@code users}. Spring Data JDBC creates instances through the single
 * constructor (its parameter names match the columns) and sets the remaining
 * columns ({@code id}, {@code created_at}, {@code verified_at}) on the fields.
 */
@Table(name = "users")
public class User {

    /** Assigned by the database on first save; {@code null} until then. */
    @Id
    private @Nullable Long id;

    private String email;

    private String username;

    private String password;

    private LocalDateTime createdAt;

    /** {@code null} until the account's email address has been verified. */
    private @Nullable LocalDateTime verifiedAt;

    public User(final String email, final String username, final String password) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.createdAt = LocalDateTime.now();
    }

    public User changeEmailAddress(final String newEmail) {
        this.email = newEmail;
        return this;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setVerifiedAt(final @Nullable LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public @Nullable Long getId() {
        return this.id;
    }

    public String getEmail() {
        return this.email;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public @Nullable LocalDateTime getVerifiedAt() {
        return this.verifiedAt;
    }
}
