package edu.northwestu.project1p2.repositories;

import edu.northwestu.project1p2.entities.EmailVerificationToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailVerificationTokenRepository extends CrudRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    /** True when a token was issued to the user after {@code cutoff} (resend cooldown). */
    boolean existsByUserIdAndCreatedAtAfter(Long userId, LocalDateTime cutoff);

    /** Marks every still-usable token for a user as used, so only one link works at a time. */
    @Modifying
    @Query("UPDATE email_verification_tokens SET used_at = :usedAt WHERE user_id = :userId AND used_at IS NULL")
    int invalidateAllForUser(@Param("userId") Long userId, @Param("usedAt") LocalDateTime usedAt);
}
