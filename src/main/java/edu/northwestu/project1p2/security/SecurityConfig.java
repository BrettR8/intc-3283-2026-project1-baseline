package edu.northwestu.project1p2.security;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.ExceptionMappingAuthenticationFailureHandler;

/**
 * Browser security: form login against the {@code users} table, a server-side
 * session, and CSRF protection on every state-changing request. An account must
 * confirm its email address before it can sign in.
 *
 * <p>Thymeleaf's {@code th:action} injects the CSRF hidden field automatically,
 * so every POST form in the templates (register, login, logout, confirm) carries it.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** The homepage: public landing page when signed out, dashboard when signed in. */
    public static final String HOME = "/";

    /** The sign-in page; also the URL Spring Security's login filter processes on POST. */
    public static final String LOGIN = "/login";

    /** Paths that must work before sign-in: front door, sign-in, registration, email confirmation, assets. */
    private static final String[] PUBLIC_PATHS = {
        HOME, LOGIN, "/register", "/error",
        "/check-email", "/verify-email/**",
        "/css/**", "/js/**", "/images/**", "/favicon.ico",
    };

    @Bean
    public SecurityFilterChain browserSecurityFilterChain(final HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage(LOGIN)
                        // The login form posts "email" rather than Spring's default "username".
                        .usernameParameter("email")
                        // Return to the originally requested page, or the dashboard.
                        .defaultSuccessUrl(HOME)
                        .failureHandler(loginFailureHandler()))
                .logout(logout -> logout
                        .logoutSuccessUrl(LOGIN + "?logout"));
        return http.build();
    }

    /**
     * Username/password provider with the email-confirmation check added after the
     * password check (see {@link VerifiedEmailChecker}).
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(final UserDetailsService userDetailsService,
                                                            final PasswordEncoder passwordEncoder) {
        final DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setPostAuthenticationChecks(new VerifiedEmailChecker());
        return provider;
    }

    /**
     * Delegating encoder: stores hashes as {@code {bcrypt}$2a$...} so the algorithm
     * can be upgraded later without invalidating existing passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** Unconfirmed account (correct password) → resend prompt; anything else → generic error. */
    private static AuthenticationFailureHandler loginFailureHandler() {
        final ExceptionMappingAuthenticationFailureHandler handler = new ExceptionMappingAuthenticationFailureHandler();
        handler.setDefaultFailureUrl(LOGIN + "?error");
        handler.setExceptionMappings(Map.of(DisabledException.class.getName(), LOGIN + "?unverified"));
        return handler;
    }
}
