package io.github.lucyechkaaa.jfinance;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Smoke test: context starts, Flyway migrates and Hibernate validates the schema. */
@SpringBootTest
@ActiveProfiles("test")
class FinanceManagerApplicationTests {

	@Test
	void contextLoads() {
	}
}
