package edu.northwestu.project1p2.security;

import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;

/**
 * Post-authentication check: rejects a correct password for an account whose email
 * is not confirmed yet. Runs only after the password matched, so the
 * "confirm your email" response is never shown to someone guessing addresses.
 */
public class VerifiedEmailChecker implements UserDetailsChecker {

    /** Spring's standard account-status checks still run first. */
    private final UserDetailsChecker accountStatus = new AccountStatusUserDetailsChecker();

    @Override
    public void check(final UserDetails user) {
        this.accountStatus.check(user);
        if (user instanceof AppUserDetails details && !details.emailVerified()) {
            throw new DisabledException("Email address has not been confirmed");
        }
    }
}
