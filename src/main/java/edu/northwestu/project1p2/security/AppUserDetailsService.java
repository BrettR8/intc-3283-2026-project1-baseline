package edu.northwestu.project1p2.security;

import edu.northwestu.project1p2.repositories.UserRepository;
import edu.northwestu.project1p2.services.EmailAddresses;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads a {@link AppUserDetails} by email for the form-login filter. Because this
 * is the only {@link UserDetailsService} bean and a {@code PasswordEncoder} bean
 * exists, Spring Security wires them into its {@code DaoAuthenticationProvider}.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(final String email) {
        return this.userRepository.findByEmail(EmailAddresses.normalize(email))
                .map(AppUserDetails::from)
                // Same message whether the email or the password is wrong, so the
                // login page can't be used to discover which emails are registered.
                .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));
    }
}
