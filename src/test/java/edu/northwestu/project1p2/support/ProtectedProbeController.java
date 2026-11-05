package edu.northwestu.project1p2.support;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only route that is NOT in {@code SecurityConfig.PUBLIC_PATHS}. The app has no
 * protected page of its own yet, so this proves a real route still requires sign-in
 * while an unknown path answers 404. Lives in test sources, so it never ships.
 */
@RestController
public class ProtectedProbeController {

    public static final String PATH = "/test-support/protected";

    @GetMapping(PATH)
    public String probe() {
        return "protected content";
    }
}
