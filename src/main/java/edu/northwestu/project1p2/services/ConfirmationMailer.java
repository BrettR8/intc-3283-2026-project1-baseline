package edu.northwestu.project1p2.services;

import edu.northwestu.project1p2.config.AppProperties;
import edu.northwestu.project1p2.entities.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import java.util.Locale;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Sends the "confirm your email" message as multipart/alternative: a plain-text
 * part plus an HTML part rendered from {@code templates/email/confirm-email.html}.
 */
@Component
public class ConfirmationMailer {

    static final String SUBJECT = "Confirm your email address";

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;
    private final AppProperties properties;
    private final Clock clock;

    public ConfirmationMailer(final JavaMailSender mailSender, final ITemplateEngine templateEngine,
                              final AppProperties properties, final Clock clock) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.properties = properties;
        this.clock = clock;
    }

    /** Sends the confirmation message; throws a {@code MailException} if it cannot. */
    public void send(final User user, final String confirmationUrl) {
        final MimeMessage message = this.mailSender.createMimeMessage();
        try {
            final MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(this.properties.mailFromAddress(), this.properties.mailFromName());
            helper.setTo(user.getEmail());
            helper.setSubject(SUBJECT);
            helper.setSentDate(Date.from(this.clock.instant()));
            helper.setText(this.textBody(user, confirmationUrl), this.htmlBody(user, confirmationUrl));
        } catch (final MessagingException | UnsupportedEncodingException e) {
            throw new MailPreparationException("Could not build the confirmation email", e);
        }
        this.mailSender.send(message);
    }

    private String textBody(final User user, final String confirmationUrl) {
        return String.join("\r\n",
                "Hi " + user.getUsername() + ",",
                "",
                "Confirm your email address to finish setting up your account:",
                "",
                confirmationUrl,
                "",
                "This link expires in " + this.properties.emailVerificationTtl().toHours() + " hours.",
                "If you didn't create an account, you can ignore this email.",
                "");
    }

    private String htmlBody(final User user, final String confirmationUrl) {
        final Context context = new Context(Locale.ENGLISH);
        context.setVariable("username", user.getUsername());
        context.setVariable("confirmationUrl", confirmationUrl);
        context.setVariable("expiresInHours", this.properties.emailVerificationTtl().toHours());
        return this.templateEngine.process("email/confirm-email", context);
    }
}
