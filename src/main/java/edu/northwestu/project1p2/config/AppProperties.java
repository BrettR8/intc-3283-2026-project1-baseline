package edu.northwestu.project1p2.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Application settings under {@code app.*} (see application.yaml).
 *
 * @param baseUrl              public URL used in links inside emails
 * @param mailFromName         display name on outgoing mail
 * @param mailFromLocalPart    mailbox part of the From address
 * @param mailFromDomain       domain part of the From address
 * @param emailVerificationTtl how long a confirmation link stays valid
 * @param emailResendCooldown  minimum gap between confirmation emails to one account
 */
@ConfigurationProperties("app")
public record AppProperties(
        @DefaultValue("http://localhost:8080") String baseUrl,
        @DefaultValue("Project 1p2") String mailFromName,
        @DefaultValue("no-reply") String mailFromLocalPart,
        @DefaultValue("project1p2.test") String mailFromDomain,
        @DefaultValue("24h") Duration emailVerificationTtl,
        @DefaultValue("60s") Duration emailResendCooldown) {

    /** The From address, assembled from its configured parts. */
    public String mailFromAddress() {
        return this.mailFromLocalPart + '@' + this.mailFromDomain;
    }

    /** Absolute confirmation link for {@code token}. */
    public String verificationUrl(final String token) {
        final String base = this.baseUrl.endsWith("/")
                ? this.baseUrl.substring(0, this.baseUrl.length() - 1)
                : this.baseUrl;
        return base + "/verify-email?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
