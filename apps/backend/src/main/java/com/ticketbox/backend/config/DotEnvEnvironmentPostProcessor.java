package com.ticketbox.backend.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class DotEnvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "dotenvFile";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dotEnvDirectory = findDotEnvDirectory();
        if (dotEnvDirectory == null) {
            return;
        }

        Dotenv dotenv = Dotenv.configure()
            .directory(dotEnvDirectory)
            .ignoreIfMissing()
            .ignoreIfMalformed()
            .load();

        Map<String, Object> dotenvProperties = new HashMap<>();
        dotenv.entries().forEach(entry -> {
            if (System.getenv(entry.getKey()) == null) {
                dotenvProperties.put(entry.getKey(), entry.getValue());
            }
        });

        if (!dotenvProperties.isEmpty()) {
            if (environment.getPropertySources().contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
                environment.getPropertySources().addAfter(
                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    new MapPropertySource(PROPERTY_SOURCE_NAME, dotenvProperties)
                );
            } else {
                environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, dotenvProperties));
            }
        }
    }

    private String findDotEnvDirectory() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null) {
            File candidate = current.resolve(".env").toFile();
            if (candidate.exists() && candidate.isFile()) {
                return current.toString();
            }
            current = current.getParent();
        }
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
