package edu.northwestu.project1p2.controllers;

import java.util.Map;

/**
 * Plain-language heading and explanation for an HTTP error status, shown on the
 * error page. Unlisted codes fall back to a generic 4xx or 5xx description.
 *
 * @param title       short heading, e.g. "Page not found"
 * @param description one or two sentences telling the visitor what happened and what to do
 */
public record ErrorDescription(String title, String description) {

    private static final ErrorDescription CLIENT_ERROR = new ErrorDescription("Request problem",
            "The request couldn't be completed. Check the address and try again.");

    private static final ErrorDescription SERVER_ERROR = new ErrorDescription("Something went wrong",
            "An unexpected error happened on our side. Try again in a moment.");

    private static final Map<Integer, ErrorDescription> KNOWN = Map.ofEntries(
            Map.entry(400, new ErrorDescription("Bad request",
                    "The request was malformed or missing information. Go back and try again.")),
            Map.entry(401, new ErrorDescription("Sign in required",
                    "You need to sign in before you can see this page.")),
            Map.entry(403, new ErrorDescription("Access denied",
                    "You don't have permission to do that. If you submitted a form, your session may have "
                            + "expired: reload the page and try again.")),
            Map.entry(404, new ErrorDescription("Page not found",
                    "The page you requested does not exist. It may have moved, or the address may be "
                            + "mistyped.")),
            Map.entry(405, new ErrorDescription("Method not allowed",
                    "This page doesn't accept that kind of request.")),
            Map.entry(408, new ErrorDescription("Request timed out",
                    "The request took too long to arrive. Try again.")),
            Map.entry(409, new ErrorDescription("Conflict",
                    "The request conflicts with the current state of the data. Reload and try again.")),
            Map.entry(413, new ErrorDescription("Request too large",
                    "What you sent is larger than this site accepts.")),
            Map.entry(415, new ErrorDescription("Unsupported format",
                    "The request used a format this page doesn't accept.")),
            Map.entry(429, new ErrorDescription("Too many requests",
                    "You're sending requests too quickly. Wait a moment, then try again.")),
            Map.entry(500, SERVER_ERROR),
            Map.entry(502, new ErrorDescription("Bad gateway",
                    "A service this site depends on gave an invalid response. Try again in a moment.")),
            Map.entry(503, new ErrorDescription("Service unavailable",
                    "The site is temporarily unavailable, probably for maintenance. Try again soon.")),
            Map.entry(504, new ErrorDescription("Gateway timeout",
                    "A service this site depends on took too long to respond. Try again in a moment.")));

    /** Description for {@code status}; a generic one for unlisted 4xx/5xx codes. */
    public static ErrorDescription forStatus(final int status) {
        final ErrorDescription fallback = status >= 400 && status < 500 ? CLIENT_ERROR : SERVER_ERROR;
        return KNOWN.getOrDefault(status, fallback);
    }
}
