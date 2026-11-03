package edu.northwestu.project1p2.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generates and hashes one-time confirmation tokens. The raw token only ever
 * exists in the emailed link; the database stores {@link #hash(String)}.
 */
public final class VerificationTokens {

    /** 256 bits of randomness: not guessable, and fits in a URL as 43 characters. */
    private static final int TOKEN_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    private VerificationTokens() {
    }

    /** A new random, URL-safe token. */
    public static String newToken() {
        final byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Hex SHA-256 of {@code token}, the form stored and looked up in the database. */
    public static String hash(final String token) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException e) {
            // Every Java platform is required to provide SHA-256.
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
