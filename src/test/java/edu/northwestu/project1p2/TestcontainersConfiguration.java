package edu.northwestu.project1p2;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    static final int SMTP_PORT = 1025;

    static final int API_PORT = 8025;

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
    }

    /**
     * smtp-tester captures the app's mail and validates it against the SMTP/MIME RFCs.
     * REJECT_ON=ERROR makes a malformed message fail the send, so tests catch it.
     */
    @Bean
    GenericContainer<?> smtpTesterContainer() {
        return new GenericContainer<>(DockerImageName.parse("ghcr.io/sparrowlogic/smtp-tester:0.1.0"))
                .withExposedPorts(SMTP_PORT, API_PORT)
                .withEnv("SMTP_TESTER_VALIDATION_REJECT_ON", "ERROR")
                .withEnv("SMTP_TESTER_SPOOL_DIRECTORY", "")
                .waitingFor(Wait.forHttp("/api/v1/inboxes").forPort(API_PORT));
    }

    /** Points spring.mail at the container's mapped SMTP port. */
    @Bean
    DynamicPropertyRegistrar smtpTesterProperties(final GenericContainer<?> smtpTesterContainer) {
        return registry -> {
            registry.add("spring.mail.host", smtpTesterContainer::getHost);
            registry.add("spring.mail.port", () -> smtpTesterContainer.getMappedPort(SMTP_PORT));
        };
    }

    @Bean
    SmtpTesterInbox smtpTesterInbox(final GenericContainer<?> smtpTesterContainer) {
        return new SmtpTesterInbox(
                "http://" + smtpTesterContainer.getHost() + ":" + smtpTesterContainer.getMappedPort(API_PORT));
    }
}
