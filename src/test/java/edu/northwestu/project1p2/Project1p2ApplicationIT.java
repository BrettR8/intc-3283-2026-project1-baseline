package edu.northwestu.project1p2;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Tag("integration")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class Project1p2ApplicationIT {

    @Test
    void contextLoads() {
    }

}
