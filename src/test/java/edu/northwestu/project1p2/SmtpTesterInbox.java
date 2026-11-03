package edu.northwestu.project1p2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.client.RestClient;

/**
 * Reads mail captured by the smtp-tester container through its REST API
 * ({@code /api/v1/messages}). Only the fields the tests use are mapped.
 */
public final class SmtpTesterInbox {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private static final Pattern TOKEN = Pattern.compile("verify-email\\?token=([A-Za-z0-9_-]+)");

    private final RestClient client;

    SmtpTesterInbox(final String baseUrl) {
        this.client = RestClient.create(baseUrl);
    }

    /** Number of messages delivered to {@code recipient}. */
    public long count(final String recipient) {
        return this.list(recipient).total();
    }

    /** Waits until {@code recipient} has at least {@code expected} messages, then returns the newest in full. */
    public Email awaitMessage(final String recipient, final int expected) throws InterruptedException {
        final Instant deadline = Instant.now().plus(TIMEOUT);
        EmailList list = this.list(recipient);
        while (list.total() < expected && Instant.now().isBefore(deadline)) {
            Thread.sleep(100);
            list = this.list(recipient);
        }
        if (list.total() < expected) {
            throw new AssertionError("Expected " + expected + " email(s) for " + recipient + " within " + TIMEOUT
                    + " but found " + list.total());
        }
        return this.client.get().uri("/api/v1/messages/{id}", list.messages().getFirst().id())
                .retrieve().body(Email.class);
    }

    /** The raw token from the confirmation link in the plain-text part. */
    public static String confirmationToken(final Email email) {
        final Matcher matcher = TOKEN.matcher(email.text());
        if (!matcher.find()) {
            throw new AssertionError("No confirmation link in email text: " + email.text());
        }
        return matcher.group(1);
    }

    private EmailList list(final String recipient) {
        return this.client.get()
                .uri(uri -> uri.path("/api/v1/messages").queryParam("inbox", recipient).build())
                .retrieve().body(EmailList.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmailList(long total, List<Summary> messages) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Summary(String id, String subject, String validationStatus, boolean rejected) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Email(Summary summary, List<String> headerFrom, List<Header> headers,
                        String text, String html, Validation validation) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(String name, String value) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Validation(String status, long errors, long warnings, List<Finding> findings) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Finding(String rule, String severity, String summary) {
    }
}
