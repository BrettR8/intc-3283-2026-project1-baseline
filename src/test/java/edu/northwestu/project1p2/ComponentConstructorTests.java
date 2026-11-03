package edu.northwestu.project1p2;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wiring guard: every scanned component must have exactly one constructor so
 * Spring picks it without {@code @Autowired}. Two un-annotated constructors is
 * a "No default constructor found" crash at startup; this catches it at build
 * time without booting the context.
 */
class ComponentConstructorTests {

    @Test
    void everyComponentHasExactlyOneConstructor() throws ClassNotFoundException {
        final ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        // @Service, @Controller, @Configuration, ... are all meta-annotated with @Component.
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));

        final List<String> offenders = new ArrayList<>();
        int scanned = 0;
        for (final BeanDefinition candidate : scanner.findCandidateComponents("edu.northwestu.project1p2")) {
            final Class<?> type = Class.forName(candidate.getBeanClassName());
            scanned++;
            if (type.getDeclaredConstructors().length != 1) {
                offenders.add(type.getName());
            }
        }

        // Guard against a vacuous pass if the scan ever finds nothing.
        assertThat(scanned).as("components scanned").isGreaterThanOrEqualTo(5);
        assertThat(offenders).as("components with more than one constructor").isEmpty();
    }
}
