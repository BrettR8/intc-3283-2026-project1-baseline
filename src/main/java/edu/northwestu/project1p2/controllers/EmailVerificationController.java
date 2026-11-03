package edu.northwestu.project1p2.controllers;

import edu.northwestu.project1p2.config.AppProperties;
import edu.northwestu.project1p2.services.EmailVerificationService;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Email confirmation pages.
 *
 * <p>The emailed link opens a page with a "Confirm" button rather than confirming
 * on GET: mail scanners and link previewers fetch links automatically, and a
 * GET that consumed the token would burn it before the user ever clicked.
 */
@Controller
public class EmailVerificationController {

    private static final String SECTION = "section";

    private static final String SECTION_KEY = "verify-email";

    private static final String CONFIRM_VIEW = "verify-email";

    private final EmailVerificationService emailVerificationService;
    private final AppProperties properties;

    public EmailVerificationController(final EmailVerificationService emailVerificationService,
                                       final AppProperties properties) {
        this.emailVerificationService = emailVerificationService;
        this.properties = properties;
    }

    /** Shown right after registration. */
    @GetMapping("/check-email")
    public String checkEmail(final Model model) {
        model.addAttribute("expiresInHours", this.properties.emailVerificationTtl().toHours());
        model.addAttribute(SECTION, SECTION_KEY);
        return "check-email";
    }

    /** Landing page for the emailed link: shows the confirm button. */
    @GetMapping("/verify-email")
    public String showConfirm(@RequestParam(defaultValue = "") final String token, final Model model) {
        model.addAttribute("token", token);
        model.addAttribute(SECTION, SECTION_KEY);
        return CONFIRM_VIEW;
    }

    @PostMapping("/verify-email")
    public String confirm(@RequestParam(defaultValue = "") final String token, final Model model) {
        final boolean confirmed = !token.isBlank() && this.emailVerificationService.confirm(token);
        model.addAttribute("failed", !confirmed);
        model.addAttribute(SECTION, SECTION_KEY);
        return confirmed ? "redirect:/login?verified" : CONFIRM_VIEW;
    }

    @GetMapping("/verify-email/resend")
    public String showResend(@RequestParam(required = false) final @Nullable String sent, final Model model) {
        model.addAttribute("sent", sent != null);
        model.addAttribute(SECTION, SECTION_KEY);
        return "resend-verification";
    }

    /** Always redirects to the same "sent" page, so it never reveals whether an email is registered. */
    @PostMapping("/verify-email/resend")
    public String resend(@RequestParam(defaultValue = "") final String email) {
        this.emailVerificationService.resend(email);
        return "redirect:/verify-email/resend?sent";
    }
}
