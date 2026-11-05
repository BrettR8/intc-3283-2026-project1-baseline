package edu.northwestu.project1p2.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * {@code GET /logout}: the page Spring Security redirects to after a successful
 * {@code POST /logout} (see {@code SecurityConfig}). The POST itself is handled by
 * the logout filter, not by this controller.
 *
 * <p>A visitor who is still signed in (for example, one who typed the URL) gets a
 * confirm button instead, because signing out must stay a CSRF-protected POST.
 */
@Controller
public class LogoutController {

    @GetMapping("/logout")
    public String logout(final Model model) {
        model.addAttribute("section", "logout");
        return "logout";
    }
}
