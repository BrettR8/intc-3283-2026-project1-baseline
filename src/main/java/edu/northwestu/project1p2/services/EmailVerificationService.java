package edu.northwestu.project1p2.services;

import edu.northwestu.project1p2.config.AppProperties;
import edu.northwestu.project1p2.entities.EmailVerificationToken;
import edu.northwestu.project1p2.entities.User;
import edu.northwestu.project1p2.repositories.EmailVerificationTokenRepository;
import edu.northwestu.project1p2.repositories.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Email confirmation: issues one-time links, consumes them, and resends on request.
 *
 * <p>Only one link per account works at a time: issuing a new one, or confirming,
 * invalidates the rest.
 */
@Service
public class EmailVerificationService {

    private static final Logger LOG = LoggerFactory.getLogger(EmailVerificationService.class);

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ConfirmationMailer mailer;
    private final AppProperties properties;
    private final Clock clock;

    public EmailVerificationService(final EmailVerificationTokenRepository tokenRepository,
                                    final UserRepository userRepository,
                                    final ConfirmationMailer mailer,
                                    final AppProperties properties,
                                    final Clock clock) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.mailer = mailer;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Issues a fresh link to a saved user and emails it. A mail failure is logged,
     * not thrown: the account exists either way and the user can ask for a resend.
     *
     * @return whether the email was handed to the mail server
     */
    public boolean sendConfirmation(final User user) {
        final Long userId = Objects.requireNonNull(user.getId(), "user must be saved first");
        final LocalDateTime now = this.now();
        this.tokenRepository.invalidateAllForUser(userId, now);
        final String token = VerificationTokens.newToken();
        this.tokenRepository.save(new EmailVerificationToken(
                userId, VerificationTokens.hash(token), now, now.plus(this.properties.emailVerificationTtl())));
        boolean sent = true;
        try {
            this.mailer.send(user, this.properties.verificationUrl(token));
        } catch (final MailException e) {
            LOG.warn("Confirmation email to user {} could not be sent", userId, e);
            sent = false;
        }
        return sent;
    }

    /**
     * Confirms the account that owns {@code rawToken} if the token is unused and
     * unexpired, then invalidates every outstanding link for that account.
     *
     * @return whether the token was valid
     */
    @Transactional
    public boolean confirm(final String rawToken) {
        final LocalDateTime now = this.now();
        final Optional<EmailVerificationToken> token = this.tokenRepository
                .findByTokenHash(VerificationTokens.hash(rawToken))
                .filter(candidate -> candidate.isUsableAt(now));
        token.ifPresent(valid -> this.markVerified(valid.getUserId(), now));
        return token.isPresent();
    }

    /**
     * Sends a new link if {@code email} belongs to an unconfirmed account and none
     * was sent within the cooldown. Silent in every other case, so the caller's
     * response never reveals whether an email address is registered.
     */
    public void resend(final String email) {
        final LocalDateTime cutoff = this.now().minus(this.properties.emailResendCooldown());
        this.userRepository.findByEmail(EmailAddresses.normalize(email))
                .filter(user -> user.getVerifiedAt() == null)
                .filter(user -> !this.tokenRepository.existsByUserIdAndCreatedAtAfter(
                        Objects.requireNonNull(user.getId()), cutoff))
                .ifPresent(this::sendConfirmation);
    }

    private void markVerified(final Long userId, final LocalDateTime now) {
        this.userRepository.findById(userId)
                .filter(user -> user.getVerifiedAt() == null)
                .ifPresent(user -> {
                    user.setVerifiedAt(now);
                    this.userRepository.save(user);
                });
        this.tokenRepository.invalidateAllForUser(userId, now);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(this.clock);
    }
}
