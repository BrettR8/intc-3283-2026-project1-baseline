package edu.northwestu.project1p2.services;

import edu.northwestu.project1p2.entities.User;
import edu.northwestu.project1p2.forms.RegistrationForm;
import edu.northwestu.project1p2.repositories.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Creates accounts from a validated {@link RegistrationForm}, hashing the password,
 * and emails a confirmation link. The account cannot sign in until it is confirmed.
 */
@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public RegistrationService(final UserRepository userRepository, final PasswordEncoder passwordEncoder,
                               final EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    public Result register(final RegistrationForm form) {
        final String email = EmailAddresses.normalize(form.getEmail());
        if (this.userRepository.existsByEmail(email)) {
            return Result.EMAIL_TAKEN;
        }
        final User user = new User(email, form.getUsername().trim(), this.passwordEncoder.encode(form.getPassword()));
        return this.save(user);
    }

    private Result save(final User user) {
        Result result = Result.REGISTERED;
        try {
            final User saved = this.userRepository.save(user);
            this.emailVerificationService.sendConfirmation(saved);
        } catch (final DataIntegrityViolationException e) {
            // Two concurrent registrations for one email: the UNIQUE constraint wins.
            result = Result.EMAIL_TAKEN;
        }
        return result;
    }

    /** Outcome of a registration attempt. */
    public enum Result { REGISTERED, EMAIL_TAKEN }
}
