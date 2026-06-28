package com.ticketbox.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import org.springframework.retry.annotation.EnableRetry;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@EnableCaching
@EnableRetry
public class Application {

	public static void main(String[] args) {
		loadDotenv();
		SpringApplication.run(Application.class, args);
	}

	private static void loadDotenv() {
		try {
			Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
			while (current != null) {
				File candidate = current.resolve(".env").toFile();
				if (candidate.exists() && candidate.isFile()) {
					Dotenv dotenv = Dotenv.configure()
							.directory(current.toString())
							.ignoreIfMissing()
							.ignoreIfMalformed()
							.load();
					dotenv.entries().forEach(entry -> {
						if (System.getProperty(entry.getKey()) == null && System.getenv(entry.getKey()) == null) {
							System.setProperty(entry.getKey(), entry.getValue());
						}
					});
					System.out.println("[DotEnv] Loaded .env file from: " + current.toString());
					break;
				}
				current = current.getParent();
			}
		} catch (Exception e) {
			System.err.println("[DotEnv] Failed to load .env file: " + e.getMessage());
		}
	}
}
