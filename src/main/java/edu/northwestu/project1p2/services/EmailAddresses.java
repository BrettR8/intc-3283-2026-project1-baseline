package edu.northwestu.project1p2.services;

import java.util.Locale;
import org.jspecify.annotations.Nullable;

/** Email normalization shared by registration and login so both look up the same row. */
public final class EmailAddresses {

    private EmailAddresses() {
    }

    /** Trims and lower-cases an email; {@code null} becomes the empty string. */
    public static String normalize(final @Nullable String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
