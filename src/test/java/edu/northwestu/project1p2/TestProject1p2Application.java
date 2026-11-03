package edu.northwestu.project1p2;

import org.springframework.boot.SpringApplication;

public class TestProject1p2Application {

    public static void main(String[] args) {
        SpringApplication.from(Project1p2Application::main).with(TestcontainersConfiguration.class).run(args);
    }

}
