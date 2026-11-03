package edu.northwestu.project1p2.controllers;

import edu.northwestu.project1p2.forms.RegistrationForm;
import edu.northwestu.project1p2.security.AppUserDetails;
import edu.northwestu.project1p2.services.RegistrationService;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/register")
public class RegisterUserController {

    /** Template name, and the active nav key for this page. */
    private static final String VIEW = "register";

    private static final String SECTION = "section";

    private final RegistrationService registrationService;

    public RegisterUserController(final RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping
    public String showRegistrationForm(@AuthenticationPrincipal final @Nullable AppUserDetails currentUser,
                                       final Model model) {
        if (currentUser != null) {
            return "redirect:/";
        }
        model.addAttribute("form", new RegistrationForm());
        model.addAttribute(SECTION, VIEW);
        return VIEW;
    }

    @PostMapping
    public String registerUser(@Valid @ModelAttribute("form") final RegistrationForm form,
                               final BindingResult bindingResult,
                               final Model model) {
        model.addAttribute(SECTION, VIEW);
        if (!form.passwordsMatch()) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        if (!bindingResult.hasErrors()
                && this.registrationService.register(form) == RegistrationService.Result.EMAIL_TAKEN) {
            bindingResult.rejectValue("email", "taken", "An account with that email already exists");
        }
        // Re-render with errors, or Post/Redirect/Get so a refresh can't resubmit the form.
        return bindingResult.hasErrors() ? VIEW : "redirect:/check-email";
    }
}
