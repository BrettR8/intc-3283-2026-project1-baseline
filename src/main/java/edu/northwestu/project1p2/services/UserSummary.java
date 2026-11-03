package edu.northwestu.project1p2.services;

import edu.northwestu.project1p2.entities.User;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/** Read-only view of a user for listings. Deliberately has no password field. */
public record UserSummary(
        @Nullable Long id, String username, String email, LocalDateTime createdAt, boolean verified) {

    public static UserSummary from(final User user) {
        return new UserSummary(
                user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt(), user.getVerifiedAt() != null);
    }
}
