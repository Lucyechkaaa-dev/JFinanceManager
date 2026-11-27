package io.github.lucyechkaaa.jfinance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lucyechkaaa.jfinance.auth.dto.RegisterRequest;
import io.github.lucyechkaaa.jfinance.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

/** Boundary values, empty input and malformed input over HTTP. */
class InputValidationIntegrationTest extends IntegrationTest {

	private MockHttpSession session;
	private String recipient;

	@BeforeEach
	void setUp() throws Exception {
		session = registerAndLogin(uniqueUsername("val"));
		recipient = uniqueUsername("rcp");
		register(recipient);
	}

	@Test
	void emptyBodyIsRejectedWithFieldErrors() throws Exception {
		mvc.perform(postJson("/api/auth/register", Map.of()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.username").exists())
				.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	void malformedJsonIsRejected() throws Exception {
		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\": "))
				.andExpect(status().isBadRequest());
	}

	@Test
	void usernameLengthBoundaries() throws Exception {
		registerExpecting(uniqueOfLength(RegisterRequest.MIN_USERNAME_LENGTH - 1), PASSWORD, 400);
		registerExpecting(uniqueOfLength(RegisterRequest.MIN_USERNAME_LENGTH), PASSWORD, 201);
		registerExpecting(uniqueOfLength(RegisterRequest.MAX_USERNAME_LENGTH), PASSWORD, 201);
		registerExpecting(uniqueOfLength(RegisterRequest.MAX_USERNAME_LENGTH + 1), PASSWORD, 400);
	}

	@Test
	void passwordLengthBoundaries() throws Exception {
		String shortest = "a1" + "b".repeat(RegisterRequest.MIN_PASSWORD_LENGTH - 2);

		registerExpecting(uniqueUsername("p"), shortest.substring(1), 400);
		registerExpecting(uniqueUsername("p"), shortest, 201);
		registerExpecting(uniqueUsername("p"), "12345678", 400);
	}

	@Test
	void zeroAndMissingAmountsAreRejected() throws Exception {
		transferExpecting(Map.of("fromCurrency", "UAH", "toUsername", recipient, "amount", "0"),
				400);
		transferExpecting(Map.of("fromCurrency", "UAH", "toUsername", recipient), 400);
		transferExpecting(Map.of("fromCurrency", "UAH", "toUsername", "  ", "amount", "1"), 400);
	}

	@Test
	void transferToUnknownUserIsNotFound() throws Exception {
		transferExpecting(Map.of("fromCurrency", "UAH", "toUsername", "nobody_" + System.nanoTime(),
				"amount", "1"), 404);
	}

	@Test
	void historyWithReversedDateRangeIsRejected() throws Exception {
		mvc.perform(get("/api/transactions").param("from", "2026-11-30").param("to", "2026-11-01")
						.session(session))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void historyWithInvalidDateIsRejected() throws Exception {
		mvc.perform(get("/api/transactions").param("from", "30.11.2026").session(session))
				.andExpect(status().isBadRequest());
	}

	@Test
	void emptyHistoryIsAnEmptyList() throws Exception {
		mvc.perform(get("/api/transactions").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void unsupportedOrDuplicateCurrencyAccountIsRejected() throws Exception {
		mvc.perform(postJson("/api/accounts", Map.of("currency", "JPY")).session(session))
				.andExpect(status().isUnprocessableContent());
		mvc.perform(postJson("/api/accounts", Map.of("currency", "UAH")).session(session))
				.andExpect(status().isConflict());
		mvc.perform(postJson("/api/accounts", Map.of("currency", "NOPE")).session(session))
				.andExpect(status().isBadRequest());
	}

	private void registerExpecting(String username, String password, int expectedStatus)
			throws Exception {
		mvc.perform(postJson("/api/auth/register",
						Map.of("username", username, "password", password)))
				.andExpect(status().is(expectedStatus));
	}

	private void transferExpecting(Map<String, String> body, int expectedStatus)
			throws Exception {
		mvc.perform(postJson("/api/transfers", body).session(session))
				.andExpect(status().is(expectedStatus));
	}

	/** Username of exactly {@code length} characters that is unique within the test run. */
	private static String uniqueOfLength(int length) {
		String unique = Long.toString(System.nanoTime(), Character.MAX_RADIX);
		String padded = "x".repeat(length) + unique;
		return padded.substring(padded.length() - length);
	}
}
