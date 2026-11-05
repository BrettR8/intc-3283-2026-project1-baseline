package edu.northwestu.project1p2;

import edu.northwestu.project1p2.entities.User;
import edu.northwestu.project1p2.repositories.UserRepository;
import edu.northwestu.project1p2.support.ProtectedProbeController;
import jakarta.servlet.RequestDispatcher;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * End-to-end auth flow through the REAL Spring Security filter chain and a real
 * Postgres (Testcontainers). Users sign in with an actual form-login POST; the
 * resulting session is reused for follow-up requests. Nothing security-related
 * is mocked.
 */
@Tag("integration")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationFlowIT {

    private static final String PASSWORD = "correct-horse-battery";

    // Test framework field injection; app code uses constructor injection only.
    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SmtpTesterInbox inbox;

    /** Unique address per call. Built with an explicit '@' char so tooling never rewrites it. */
    private static String uniqueEmail(final String local) {
        return local + "-" + UUID.randomUUID().toString().substring(0, 8) + '@' + "example.test";
    }

    /** Registers, then confirms the address through the real email, so the account can sign in. */
    private void register(final String username, final String email) throws Exception {
        this.mvc.perform(post("/register").with(csrf())
                        .param("username", username)
                        .param("email", email)
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD))
                .andExpect(redirectedUrl("/check-email"));

        final String recipient = email.trim().toLowerCase();
        final String token = SmtpTesterInbox.confirmationToken(this.inbox.awaitMessage(recipient, 1));
        this.mvc.perform(post("/verify-email").with(csrf()).param("token", token))
                .andExpect(redirectedUrl("/login?verified"));
    }

    private MockHttpSession signIn(final String email) throws Exception {
        final MvcResult result = this.mvc.perform(formLogin("/login").userParameter("email")
                        .user(email).password(PASSWORD))
                .andExpect(authenticated())
                .andExpect(redirectedUrl("/"))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void anonymousHomeShowsPublicNavigation() throws Exception {
        this.mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("href=\"/login\"")))
                .andExpect(content().string(containsString("href=\"/register\"")))
                .andExpect(content().string(not(containsString("Sign out"))))
                .andExpect(content().string(not(containsString("Find users"))));
    }

    @Test
    void anonymousSearchQueryNeverReturnsUserData() throws Exception {
        final String email = uniqueEmail("hidden");
        register("hidden-user", email);

        this.mvc.perform(get("/").param("q", "hidden"))
                .andExpect(view().name("index"))
                .andExpect(model().attributeDoesNotExist("users"))
                .andExpect(content().string(not(containsString(email))));
    }

    @Test
    void registrationStoresHashedPasswordAndNormalizedEmail() throws Exception {
        final String email = uniqueEmail("hash");
        register("hasher", "  " + email.toUpperCase() + " ");

        final User stored = this.userRepository.findByEmail(email).orElseThrow();
        assertThat(stored.getUsername()).isEqualTo("hasher");
        assertThat(stored.getPassword()).startsWith("{bcrypt}").isNotEqualTo(PASSWORD);
        assertThat(this.passwordEncoder.matches(PASSWORD, stored.getPassword())).isTrue();
    }

    @Test
    void registerThenSignInShowsAuthenticatedDashboardAndNavigation() throws Exception {
        final String email = uniqueEmail("dash");
        register("Dashboard Dana", email);
        final MockHttpSession session = signIn(email);

        this.mvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("index-authenticated"))
                .andExpect(content().string(containsString("Dashboard Dana")))
                .andExpect(content().string(containsString("Sign out")))
                .andExpect(content().string(containsString("Find users")))
                .andExpect(content().string(not(containsString("href=\"/register\""))));
    }

    @Test
    void dashboardFindsUsersByUsernameAndEmailWithoutLeakingPasswords() throws Exception {
        final String token = UUID.randomUUID().toString().substring(0, 8);
        final String findableEmail = uniqueEmail("findme" + token);
        final String otherEmail = uniqueEmail("other");
        register("Searchable " + token, findableEmail);
        register("Unrelated", otherEmail);
        final MockHttpSession session = signIn(otherEmail);

        // By username fragment, case-insensitive.
        this.mvc.perform(get("/").param("q", "SEARCHABLE " + token).session(session))
                .andExpect(content().string(containsString(findableEmail)))
                .andExpect(content().string(not(containsString("{bcrypt}"))));

        // By email fragment.
        this.mvc.perform(get("/").param("q", "findme" + token).session(session))
                .andExpect(content().string(containsString("Searchable " + token)))
                .andExpect(content().string(not(containsString(otherEmail + "</td>"))));

        // A LIKE wildcard is matched literally, not as "everything".
        this.mvc.perform(get("/").param("q", "%").session(session))
                .andExpect(content().string(not(containsString(findableEmail))));
    }

    @Test
    void wrongPasswordRedirectsToLoginError() throws Exception {
        final String email = uniqueEmail("wrong");
        register("wrong-pw", email);

        this.mvc.perform(formLogin("/login").userParameter("email").user(email).password("not-the-password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));

        this.mvc.perform(get("/login").param("error", ""))
                .andExpect(content().string(containsString("Invalid email or password.")));
    }

    @Test
    void unknownEmailIsRejectedLikeAWrongPassword() throws Exception {
        this.mvc.perform(formLogin("/login").userParameter("email").user(uniqueEmail("nobody")).password(PASSWORD))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void duplicateEmailIsRejectedWithFieldError() throws Exception {
        final String email = uniqueEmail("dupe");
        register("first", email);

        this.mvc.perform(post("/register").with(csrf())
                        .param("username", "second")
                        .param("email", email)
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrorCode("form", "email", "taken"));
    }

    @Test
    void invalidRegistrationShowsErrorsAndDoesNotEchoPassword() throws Exception {
        this.mvc.perform(post("/register").with(csrf())
                        .param("username", "")
                        .param("email", "not-an-email")
                        .param("password", "short")
                        .param("confirmPassword", "different"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("form", "username", "email", "password", "confirmPassword"))
                .andExpect(content().string(not(containsString("value=\"short\""))));
    }

    @Test
    void postWithoutCsrfTokenIsForbidden() throws Exception {
        this.mvc.perform(post("/register")
                        .param("username", "csrf")
                        .param("email", uniqueEmail("csrf"))
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedPathRedirectsAnonymousUserToLogin() throws Exception {
        this.mvc.perform(get(ProtectedProbeController.PATH))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void unknownPathIsNotFoundEvenWhenSignedOut() throws Exception {
        this.mvc.perform(get("/some/unknown/page"))
                .andExpect(status().isNotFound());
    }

    @Test
    void signedInUserIsRedirectedAwayFromLoginAndRegister() throws Exception {
        final String email = uniqueEmail("redirect");
        register("redirector", email);
        final MockHttpSession session = signIn(email);

        this.mvc.perform(get("/login").session(session)).andExpect(redirectedUrl("/"));
        this.mvc.perform(get("/register").session(session)).andExpect(redirectedUrl("/"));
    }

    @Test
    void logoutEndsSessionAndRestoresPublicNavigation() throws Exception {
        final String email = uniqueEmail("logout");
        register("leaver", email);
        final MockHttpSession session = signIn(email);

        this.mvc.perform(post("/logout").with(csrf()).session(session))
                .andExpect(redirectedUrl("/logout"))
                .andExpect(unauthenticated());

        this.mvc.perform(get("/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("logout"))
                .andExpect(content().string(containsString("You have been signed out.")));

        this.mvc.perform(get("/").session(session))
                .andExpect(view().name("index"))
                .andExpect(content().string(not(containsString("Sign out"))));
    }

    @Test
    void signedInGetLogoutAsksForConfirmationAndDoesNotSignOut() throws Exception {
        final String email = uniqueEmail("confirm-logout");
        register("stayer", email);
        final MockHttpSession session = signIn(email);

        this.mvc.perform(get("/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("logout"))
                .andExpect(content().string(containsString("Are you sure you want to sign out?")))
                .andExpect(content().string(not(containsString("You have been signed out."))))
                .andExpect(authenticated());
    }

    @Test
    void unhandledExceptionRendersErrorPageWithStackTrace() throws Exception {
        this.mvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/boom")
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("kaboom <b>")))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString("java.lang.IllegalStateException")))
                .andExpect(content().string(containsString("kaboom &lt;b&gt;")))
                .andExpect(content().string(containsString("Stack trace")))
                .andExpect(content().string(containsString("AuthenticationFlowIT")))
                .andExpect(content().string(not(containsString("Whitelabel"))));
    }

    @Test
    void unknownPageRendersNotFoundErrorPage() throws Exception {
        this.mvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/missing"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(content().string(containsString("The page you requested does not exist.")))
                .andExpect(content().string(containsString("/missing")));
    }
}
