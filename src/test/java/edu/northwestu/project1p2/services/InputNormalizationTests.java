package edu.northwestu.project1p2.services;

import edu.northwestu.project1p2.forms.RegistrationForm;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit tests for input normalization; no Spring context or database. */
class InputNormalizationTests {

    @Test
    void emailIsTrimmedAndLowerCased() {
        assertThat(EmailAddresses.normalize("  Mixed.Case" + '@' + "Example.TEST ")).isEqualTo("mixed.case" + '@' + "example.test");
    }

    @Test
    void nullEmailNormalizesToEmpty() {
        assertThat(EmailAddresses.normalize(null)).isEmpty();
    }

    @Test
    void searchQueryIsTrimmedAndCapped() {
        assertThat(UserDirectoryService.normalizeQuery(null)).isEmpty();
        assertThat(UserDirectoryService.normalizeQuery("   ")).isEmpty();
        assertThat(UserDirectoryService.normalizeQuery("  ada ")).isEqualTo("ada");
        assertThat(UserDirectoryService.normalizeQuery("x".repeat(500)))
                .hasSize(UserDirectoryService.MAX_QUERY_LENGTH);
    }

    @Test
    void registrationFormTrimsEmailAndComparesPasswords() {
        final RegistrationForm form = new RegistrationForm();
        form.setEmail("  someone" + '@' + "example.test  ");
        form.setPassword("secret-password");
        form.setConfirmPassword("secret-password");
        assertThat(form.getEmail()).isEqualTo("someone" + '@' + "example.test");
        assertThat(form.passwordsMatch()).isTrue();

        form.setConfirmPassword("different");
        assertThat(form.passwordsMatch()).isFalse();

        form.setEmail(null);
        assertThat(form.getEmail()).isEmpty();
    }
}
