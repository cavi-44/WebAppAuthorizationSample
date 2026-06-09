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
        // Load .env file from the backend root directory on startup
        try {
            Path envPath = Paths.get(".env");
            if (Files.exists(envPath)) {
                Files.readAllLines(envPath).forEach(line -> {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                        int eqIndex = trimmed.indexOf("=");
                        String key = trimmed.substring(0, eqIndex).trim();
                        String value = trimmed.substring(eqIndex + 1).trim();
                        // Strip surrounding quotes if present
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        } else if (value.startsWith("'") && value.endsWith("'")) {
                            value = value.substring(1, value.length() - 1);
                        }
                        System.setProperty(key, value);
                    }
                });
                System.out.println("Loaded environment variables from .env successfully.");
            }
        } catch (Exception e) {
            System.err.println("Could not load .env file: " + e.getMessage());
        }

        SpringApplication.run(BackendApplication.class, args);
    }
}
