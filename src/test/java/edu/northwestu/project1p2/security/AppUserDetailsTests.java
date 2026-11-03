package edu.northwestu.project1p2.security;

import edu.northwestu.project1p2.entities.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class AppUserDetailsTests {

    private static final String EMAIL = "ada" + '@' + "example.test";

    @Test
    void principalLogsInByEmailAndCarriesDisplayName() {
        final AppUserDetails details = AppUserDetails.from(new User(EMAIL, "Ada", "{bcrypt}hash"));

        assertThat(details.getUsername()).isEqualTo(EMAIL);
        assertThat(details.getDisplayName()).isEqualTo("Ada");
        assertThat(details.getPassword()).isEqualTo("{bcrypt}hash");
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly(AppUserDetails.ROLE_USER);
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void toStringNeverIncludesThePasswordHash() {
        final AppUserDetails details = AppUserDetails.from(new User(EMAIL, "Ada", "{bcrypt}secret-hash"));

        assertThat(details.toString()).contains(EMAIL).doesNotContain("secret-hash");
    }
}
