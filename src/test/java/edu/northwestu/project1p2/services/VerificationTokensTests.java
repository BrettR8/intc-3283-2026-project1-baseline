package edu.northwestu.project1p2.services;

import edu.northwestu.project1p2.config.AppProperties;
import edu.northwestu.project1p2.entities.EmailVerificationToken;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for token generation, hashing, expiry and link building. */
class VerificationTokensTests {

    @Test
    void tokensAreUrlSafeAndUnique() {
        final Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            final String token = VerificationTokens.newToken();
            assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
            assertThat(seen.add(token)).isTrue();
        }
    }

    @Test
    void hashIsStableHexSha256() {
        // SHA-256("abc") from FIPS 180-2.
        assertThat(VerificationTokens.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(VerificationTokens.hash("abc")).isEqualTo(VerificationTokens.hash("abc"));
    }

    @Test
    void tokenIsUsableUntilExpiryAndNotAfter() {
        final LocalDateTime created = LocalDateTime.of(2026, 1, 1, 12, 0);
        final EmailVerificationToken token =
                new EmailVerificationToken(1L, "hash", created, created.plusHours(24));

        assertThat(token.isUsableAt(created.plusHours(23))).isTrue();
        assertThat(token.isUsableAt(created.plusHours(24))).isFalse();
    }

    @Test
    void verificationUrlUsesConfiguredBaseUrlAndAssemblesFromAddress() {
        final AppProperties properties = new AppProperties(
                "https://app.example.test/", "Example", "no-reply", "example.test",
                Duration.ofHours(24), Duration.ofSeconds(60));

        assertThat(properties.verificationUrl("abc_-1"))
                .isEqualTo("https://app.example.test/verify-email?token=abc_-1");
        assertThat(properties.mailFromAddress()).isEqualTo("no-reply" + '@' + "example.test");
    }
}
