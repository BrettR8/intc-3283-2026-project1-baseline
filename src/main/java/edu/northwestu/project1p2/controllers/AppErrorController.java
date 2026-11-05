package edu.northwestu.project1p2.controllers;

import edu.northwestu.project1p2.config.AppProperties;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.ModelAndView;

/**
 * Handles every error the servlet container forwards to {@code /error}: exceptions
 * thrown by controllers, view rendering, or security filters, plus plain error
 * statuses such as 404. Replaces Spring Boot's Whitelabel page (an application
 * {@link ErrorController} bean switches off {@code BasicErrorController}).
 *
 * <p>Browsers get the {@code error} template; other clients get JSON. When
 * {@code app.show-stack-traces} is on, both include the exception type, message,
 * and full stack trace.
 */
@Controller
@RequestMapping("/error")
public class AppErrorController implements ErrorController {

    private final ErrorAttributes errorAttributes;

    private final AppProperties properties;

    public AppErrorController(final ErrorAttributes errorAttributes, final AppProperties properties) {
        this.errorAttributes = errorAttributes;
        this.properties = properties;
    }

    @RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView errorHtml(final HttpServletRequest request) {
        final HttpStatus status = status(request);
        final ModelAndView view = new ModelAndView("error", this.attributes(request), status);
        final ErrorDescription description = ErrorDescription.forStatus(status.value());
        view.addObject("title", description.title());
        view.addObject("description", description.description());
        view.addObject("serverError", status.is5xxServerError());
        view.addObject("section", "error");
        return view;
    }

    @RequestMapping
    public ResponseEntity<Map<String, @Nullable Object>> error(final HttpServletRequest request) {
        return ResponseEntity.status(status(request)).body(this.attributes(request));
    }

    /** timestamp, status, error, path; plus exception, message, and trace when enabled. */
    private Map<String, @Nullable Object> attributes(final HttpServletRequest request) {
        ErrorAttributeOptions options = ErrorAttributeOptions.defaults();
        if (this.properties.showStackTraces()) {
            options = options.including(Include.EXCEPTION, Include.MESSAGE, Include.STACK_TRACE);
        }
        return this.errorAttributes.getErrorAttributes(new ServletWebRequest(request), options);
    }

    /** The status the container recorded for this error dispatch; 500 if missing or unknown. */
    private static HttpStatus status(final HttpServletRequest request) {
        final Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (code instanceof Integer value) {
            final HttpStatus resolved = HttpStatus.resolve(value);
            if (resolved != null) {
                return resolved;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
