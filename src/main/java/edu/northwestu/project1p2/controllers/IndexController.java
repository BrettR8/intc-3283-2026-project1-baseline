package edu.northwestu.project1p2.controllers;

import edu.northwestu.project1p2.security.AppUserDetails;
import edu.northwestu.project1p2.services.UserDirectoryService;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * {@code /} is public: signed-out visitors get the landing page, signed-in users
 * get the dashboard. {@code currentUser} is {@code null} for anonymous requests
 * (the anonymous principal is not an {@link AppUserDetails}), so user data is
 * only ever loaded for an authenticated session.
 */
@Controller
public class IndexController {

    private final UserDirectoryService userDirectory;

    public IndexController(final UserDirectoryService userDirectory) {
        this.userDirectory = userDirectory;
    }

    @GetMapping("/")
    public String index(@AuthenticationPrincipal final @Nullable AppUserDetails currentUser,
                        @RequestParam(name = "q", defaultValue = "") final String query,
                        final Model model) {
        if (currentUser == null) {
            model.addAttribute("section", "home");
            return "index";
        }
        model.addAttribute("section", "dashboard");
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("query", query.trim());
        model.addAttribute("users", this.userDirectory.search(query));
        model.addAttribute("totalUsers", this.userDirectory.count());
        return "index-authenticated";
    }
}
