package io.github.lucyechkaaa.jfinance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the JFinanceManager REST application.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class FinanceManagerApplication {

	/**
	 * Starts the application.
	 *
	 * @param args command-line arguments passed to Spring Boot
	 */
	public static void main(String[] args) {
		SpringApplication.run(FinanceManagerApplication.class, args);
	}
}
