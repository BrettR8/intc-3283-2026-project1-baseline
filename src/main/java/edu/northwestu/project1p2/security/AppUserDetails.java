package edu.northwestu.project1p2.security;

import edu.northwestu.project1p2.entities.User;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security principal backed by a {@code users} row. Carries the user's
 * {@link #id} and display name so controllers and templates can use them
 * without reloading the row, and without exposing the persistence entity.
 *
 * <p>The login identifier ({@link #getUsername()}) is the email address.
 *
 * <p>{@link #isEnabled()} stays {@code true} for unconfirmed accounts on purpose:
 * Spring checks it BEFORE the password, so using it would reveal which emails are
 * registered. {@link VerifiedEmailChecker} enforces confirmation after the
 * password matches instead.
 */
public record AppUserDetails(
        @Nullable Long id,
        String email,
        String displayName,
        String passwordHash,
        boolean emailVerified,
        Collection<? extends GrantedAuthority> grants
) implements UserDetails, Serializable {

    /** Authority granted to every registered account. */
    public static final String ROLE_USER = "ROLE_USER";

    @Serial
    private static final long serialVersionUID = 1L;

    public static AppUserDetails from(final User user) {
        return new AppUserDetails(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getPassword(),
                user.getVerifiedAt() != null,
                List.of(new SimpleGrantedAuthority(ROLE_USER)));
    }

    /** JavaBean-style accessor so templates can use {@code principal.displayName}. */
    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.grants;
    }

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    /** Never render the password hash, even in logs. */
    @Override
    public String toString() {
        return "AppUserDetails[id=" + this.id + ", email=" + this.email + "]";
    }
}
