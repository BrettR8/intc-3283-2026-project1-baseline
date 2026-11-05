package edu.northwestu.project1p2.controllers;

import edu.northwestu.project1p2.config.AppProperties;
import jakarta.servlet.RequestDispatcher;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;

class AppErrorControllerTests {

    private static AppErrorController controller(final boolean showStackTraces) {
        final AppProperties properties = new AppProperties("http://localhost:8080", "Project 1p2", "no-reply",
                "project1p2.test", Duration.ofHours(24), Duration.ofSeconds(60), showStackTraces);
        return new AppErrorController(new DefaultErrorAttributes(), properties);
    }

    private static MockHttpServletRequest failedRequest(final Integer statusCode) {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
        if (statusCode != null) {
            request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, statusCode);
        }
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/boom");
        request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("kaboom"));
        return request;
    }

    @Test
    void htmlErrorIncludesExceptionAndStackTraceWhenEnabled() {
        final ModelAndView view = controller(true).errorHtml(failedRequest(500));

        assertThat(view.getViewName()).isEqualTo("error");
        assertThat(view.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(view.getModel())
                .containsEntry("status", 500)
                .containsEntry("path", "/boom")
                .containsEntry("exception", IllegalStateException.class.getName())
                .containsEntry("message", "kaboom")
                .containsEntry("section", "error");
        assertThat((String) view.getModel().get("trace"))
                .startsWith("java.lang.IllegalStateException: kaboom")
                .contains(AppErrorControllerTests.class.getName());
    }

    @Test
    void jsonErrorOmitsInternalsWhenDisabled() {
        final ResponseEntity<Map<String, Object>> response = controller(false).error(failedRequest(503));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody())
                .containsEntry("status", 503)
                .doesNotContainKeys("trace", "exception", "message");
    }

    @Test
    void missingStatusDefaultsToInternalServerError() {
        assertThat(controller(false).errorHtml(failedRequest(null)).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
