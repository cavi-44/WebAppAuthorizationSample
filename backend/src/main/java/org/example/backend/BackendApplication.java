package org.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

    public static void main(String[] args) {

        SpringApplication.run(BackendApplication.class, args);
    }
}
