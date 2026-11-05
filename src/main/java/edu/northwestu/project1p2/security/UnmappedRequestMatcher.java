package edu.northwestu.project1p2.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

/**
 * Matches requests whose path no controller is mapped to.
 *
 * <p>{@code SecurityConfig} permits these, so a URL that doesn't exist answers
 * 404 for everyone instead of sending a signed-out visitor to the login page
 * first. Paths that ARE mapped are unaffected and stay protected. A path that is
 * mapped for a different HTTP method counts as mapped, so it keeps its protection
 * and answers 405 once signed in.
 *
 * <p>Static files are not controller routes, so a missing one (e.g.
 * {@code /css/nope.css}) also reaches the resource handler and gets a 404.
 */
public final class UnmappedRequestMatcher implements RequestMatcher {

    /** Lazy: the handler mapping is built after the security configuration. */
    private final ObjectProvider<RequestMappingHandlerMapping> handlerMapping;

    public UnmappedRequestMatcher(final ObjectProvider<RequestMappingHandlerMapping> handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @Override
    public boolean matches(final HttpServletRequest request) {
        // Path-pattern matching needs the parsed request path, which the
        // DispatcherServlet normally sets later. Parse it here and clean up after.
        final boolean parsedHere = !ServletRequestPathUtils.hasParsedRequestPath(request);
        if (parsedHere) {
            ServletRequestPathUtils.parseAndCache(request);
        }
        try {
            return this.handlerMapping.getObject().getHandlerMethods().keySet().stream()
                    .noneMatch(mapping -> pathMatches(mapping, request));
        } finally {
            if (parsedHere) {
                ServletRequestPathUtils.clearParsedRequestPath(request);
            }
        }
    }

    /** Compares only the path, ignoring HTTP method, params, headers and media types. */
    private static boolean pathMatches(final RequestMappingInfo mapping, final HttpServletRequest request) {
        return mapping.getActivePatternsCondition().getMatchingCondition(request) != null;
    }
}
