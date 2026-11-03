package edu.northwestu.project1p2.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Application-wide beans. */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    /** Injected wherever "now" matters, so token expiry can be tested deterministically. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
