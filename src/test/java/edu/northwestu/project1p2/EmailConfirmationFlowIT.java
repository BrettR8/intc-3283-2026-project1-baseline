package edu.northwestu.project1p2;

import edu.northwestu.project1p2.entities.EmailVerificationToken;
import edu.northwestu.project1p2.entities.User;
import edu.northwestu.project1p2.repositories.EmailVerificationTokenRepository;
import edu.northwestu.project1p2.repositories.UserRepository;
import edu.northwestu.project1p2.services.VerificationTokens;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Email confirmation end to end: real security filter chain, real Postgres, and
 * real SMTP delivery into smtp-tester, which also checks every message against
 * the SMTP/MIME RFCs. Links are read out of the delivered email.
 */
@Tag("integration")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class EmailConfirmationFlowIT {

    private static final String PASSWORD = "correct-horse-battery";

    // Test framework field injection; app code uses constructor injection only.
    @Autowired
    private MockMvc mvc;

    @Autowired
    private SmtpTesterInbox inbox;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private JdbcClient jdbc;

    private static String uniqueEmail(final String local) {
        return local + "-" + UUID.randomUUID().toString().substring(0, 8) + '@' + "example.test";
    }

    private void register(final String email) throws Exception {
        this.mvc.perform(post("/register").with(csrf())
                        .param("username", "Confirm Me")
                        .param("email", email)
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD))
                .andExpect(redirectedUrl("/check-email"));
    }

    private void confirm(final String token) throws Exception {
        this.mvc.perform(post("/verify-email").with(csrf()).param("token", token))
                .andExpect(redirectedUrl("/login?verified"));
    }

    private void expectConfirmFails(final String token) throws Exception {
        this.mvc.perform(post("/verify-email").with(csrf()).param("token", token))
                .andExpect(view().name("verify-email"))
                .andExpect(model().attribute("failed", true))
                .andExpect(content().string(containsString("invalid, has expired, or has already been used")));
    }

    private boolean isVerified(final String email) {
        return this.userRepository.findByEmail(email).orElseThrow().getVerifiedAt() != null;
    }

    /** Moves the user's tokens back in time so the resend cooldown has passed. */
    private void backdateTokens(final String email) {
        final Long userId = this.userRepository.findByEmail(email).orElseThrow().getId();
        this.jdbc.sql("UPDATE email_verification_tokens SET created_at = created_at - INTERVAL '10 minutes' "
                        + "WHERE user_id = :userId")
                .param("userId", userId)
                .update();
    }

    @Test
    void registrationSendsRfcCompliantConfirmationEmail() throws Exception {
        final String email = uniqueEmail("rfc");
        register(email);

        final SmtpTesterInbox.Email message = this.inbox.awaitMessage(email, 1);
        assertThat(message.summary().subject()).isEqualTo("Confirm your email address");
        assertThat(message.summary().rejected()).isFalse();
        assertThat(message.validation().errors())
                .as("RFC findings: %s", message.validation().findings()).isZero();
        assertThat(message.validation().status()).isEqualTo("PASS");
        // headerFrom is the parsed mailbox; the display name lives on the raw From header.
        assertThat(message.headerFrom()).containsExactly("no-reply" + '@' + "project1p2.test");
        assertThat(message.headers())
                .filteredOn(header -> "From".equalsIgnoreCase(header.name()))
                .singleElement()
                .satisfies(header -> assertThat(header.value()).contains("Project 1p2"));

        final String token = SmtpTesterInbox.confirmationToken(message);
        assertThat(message.text()).contains("http://localhost:8080/verify-email?token=" + token);
        assertThat(message.html()).contains("verify-email?token=" + token).contains("Confirm email address");
        assertThat(isVerified(email)).isFalse();
    }

    @Test
    void onlyAHashOfTheTokenIsStored() throws Exception {
        final String email = uniqueEmail("hash");
        register(email);
        final String token = SmtpTesterInbox.confirmationToken(this.inbox.awaitMessage(email, 1));

        assertThat(this.tokenRepository.findByTokenHash(token)).isEmpty();
        assertThat(this.tokenRepository.findByTokenHash(VerificationTokens.hash(token))).isPresent();
    }

    @Test
    void unconfirmedAccountCannotSignInWithCorrectPassword() throws Exception {
        final String email = uniqueEmail("unconfirmed");
        register(email);

        this.mvc.perform(formLogin("/login").userParameter("email").user(email).password(PASSWORD))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?unverified"));

        this.mvc.perform(get("/login").param("unverified", ""))
                .andExpect(content().string(containsString("Confirm your email address before signing in")))
                .andExpect(content().string(containsString("href=\"/verify-email/resend\"")));
    }

    @Test
    void unconfirmedAccountWithWrongPasswordGetsTheGenericError() throws Exception {
        final String email = uniqueEmail("guess");
        register(email);

        // Must not reveal "unverified" (and so that the email is registered) without the password.
        this.mvc.perform(formLogin("/login").userParameter("email").user(email).password("wrong-password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void openingTheLinkDoesNotConfirmUntilTheButtonIsPressed() throws Exception {
        final String email = uniqueEmail("prefetch");
        register(email);
        final String token = SmtpTesterInbox.confirmationToken(this.inbox.awaitMessage(email, 1));

        // A link scanner's GET only renders the button.
        this.mvc.perform(get("/verify-email").param("token", token))
                .andExpect(view().name("verify-email"))
                .andExpect(content().string(containsString("Confirm email address")));
        assertThat(isVerified(email)).isFalse();

        confirm(token);
        assertThat(isVerified(email)).isTrue();

        this.mvc.perform(formLogin("/login").userParameter("email").user(email).password(PASSWORD))
                .andExpect(authenticated())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void tokenWorksOnlyOnce() throws Exception {
        final String email = uniqueEmail("once");
        register(email);
        final String token = SmtpTesterInbox.confirmationToken(this.inbox.awaitMessage(email, 1));

        confirm(token);
        expectConfirmFails(token);
    }

    @Test
    void unknownAndBlankTokensAreRejected() throws Exception {
        expectConfirmFails(VerificationTokens.newToken());
        expectConfirmFails("");
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        final String email = uniqueEmail("expired");
        register(email);
        final User user = this.userRepository.findByEmail(email).orElseThrow();
        final String token = VerificationTokens.newToken();
        final LocalDateTime past = LocalDateTime.now().minusDays(2);
        this.tokenRepository.save(new EmailVerificationToken(
                user.getId(), VerificationTokens.hash(token), past, past.plusHours(24)));

        expectConfirmFails(token);
        assertThat(isVerified(email)).isFalse();
    }

    @Test
    void resendIssuesANewLinkAndInvalidatesTheOldOne() throws Exception {
        final String email = uniqueEmail("resend");
        register(email);
        final String first = SmtpTesterInbox.confirmationToken(this.inbox.awaitMessage(email, 1));
        backdateTokens(email);

        this.mvc.perform(post("/verify-email/resend").with(csrf()).param("email", email))
                .andExpect(redirectedUrl("/verify-email/resend?sent"));
        final String second = SmtpTesterInbox.confirmationToken(this.inbox.awaitMessage(email, 2));

        assertThat(second).isNotEqualTo(first);
        expectConfirmFails(first);
        confirm(second);
    }

    @Test
    void resendIsThrottledByTheCooldown() throws Exception {
        final String email = uniqueEmail("cooldown");
        register(email);
        this.inbox.awaitMessage(email, 1);

        this.mvc.perform(post("/verify-email/resend").with(csrf()).param("email", email))
                .andExpect(redirectedUrl("/verify-email/resend?sent"));

        assertThat(this.inbox.count(email)).isEqualTo(1);
    }

    @Test
    void resendForUnknownOrConfirmedAddressesLooksTheSameAndSendsNothing() throws Exception {
        final String unknown = uniqueEmail("nobody");
        this.mvc.perform(post("/verify-email/resend").with(csrf()).param("email", unknown))
                .andExpect(redirectedUrl("/verify-email/resend?sent"));
        assertThat(this.inbox.count(unknown)).isZero();

        final String email = uniqueEmail("already");
        register(email);
        confirm(SmtpTesterInbox.confirmationToken(this.inbox.awaitMessage(email, 1)));
        backdateTokens(email);
        this.mvc.perform(post("/verify-email/resend").with(csrf()).param("email", email))
                .andExpect(redirectedUrl("/verify-email/resend?sent"));
        assertThat(this.inbox.count(email)).isEqualTo(1);
    }

    @Test
    void confirmationPagesArePublic() throws Exception {
        this.mvc.perform(get("/check-email")).andExpect(view().name("check-email"));
        this.mvc.perform(get("/verify-email/resend")).andExpect(view().name("resend-verification"));
        this.mvc.perform(get("/verify-email/resend").param("sent", ""))
                .andExpect(content().string(containsString("a new link is on its way")));
    }
}
