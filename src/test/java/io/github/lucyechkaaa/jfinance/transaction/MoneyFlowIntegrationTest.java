package io.github.lucyechkaaa.jfinance.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lucyechkaaa.jfinance.support.IntegrationTest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;

/** End-to-end money flows over HTTP. Fixed test rates: 1 USD = 40 UAH, 1 EUR = 50 UAH. */
class MoneyFlowIntegrationTest extends IntegrationTest {

	private static final int MAX_DESCRIPTION_LENGTH = 255;

	private MockHttpSession admin;
	private String alice;
	private MockHttpSession aliceSession;
	private String bob;
	private MockHttpSession bobSession;

	@BeforeEach
	void setUp() throws Exception {
		admin = loginAdmin();
		alice = uniqueUsername("alice");
		aliceSession = registerAndLogin(alice);
		bob = uniqueUsername("bob");
		bobSession = registerAndLogin(bob);
		deposit(accountId(aliceSession, "UAH"), "1000.00");
	}

	@Test
	void newUserHasZeroUahAccountAndAdminDepositIncreasesIt() throws Exception {
		assertThat(balance(bobSession, "UAH")).isEqualByComparingTo("0");
		assertThat(balance(aliceSession, "UAH")).isEqualByComparingTo("1000.00");
	}

	@Test
	void transferMovesMoneyBetweenUsers() throws Exception {
		transfer(aliceSession, Map.of("fromCurrency", "UAH", "toUsername", bob, "amount", "250.50"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("TRANSFER"))
				.andExpect(jsonPath("$.debit.amount").value(250.50))
				.andExpect(jsonPath("$.debit.positive").doesNotExist())
				.andExpect(jsonPath("$.exchangeRate").doesNotExist());

		assertThat(balance(aliceSession, "UAH")).isEqualByComparingTo("749.50");
		assertThat(balance(bobSession, "UAH")).isEqualByComparingTo("250.50");
	}

	@Test
	void transferDescriptionIsStoredAndReadBackUnchanged() throws Exception {
		String description = "Оплата за квартиру №5, \"листопад\"";
		Map<String, String> body = new HashMap<>(uah(bob, "10"));
		body.put("description", description);

		transfer(aliceSession, body)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.description").value(description));

		mvc.perform(get("/api/transactions").session(bobSession))
				.andExpect(jsonPath("$[0].description").value(description));
	}

	@Test
	void transferWithoutDescriptionHasNoDescription() throws Exception {
		transfer(aliceSession, uah(bob, "10"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.description").doesNotExist());
	}

	@Test
	void tooLongDescriptionIsRejected() throws Exception {
		Map<String, String> body = new HashMap<>(uah(bob, "10"));
		body.put("description", "x".repeat(MAX_DESCRIPTION_LENGTH + 1));

		transfer(aliceSession, body).andExpect(status().isBadRequest());
		assertThat(balance(aliceSession, "UAH")).isEqualByComparingTo("1000.00");
	}

	@Test
	void insufficientFundsIsRejectedAndNothingChanges() throws Exception {
		transfer(aliceSession, uah(bob, "1000.01"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.detail").value(containsString("Insufficient")));

		assertThat(balance(aliceSession, "UAH")).isEqualByComparingTo("1000.00");
		assertThat(balance(bobSession, "UAH")).isEqualByComparingTo("0");
	}

	@Test
	void invalidAmountsAreRejected() throws Exception {
		transfer(aliceSession, Map.of("fromCurrency", "UAH", "toUsername", bob, "amount", "-5"))
				.andExpect(status().isBadRequest());
		transfer(aliceSession, Map.of("fromCurrency", "UAH", "toUsername", bob, "amount", "1.001"))
				.andExpect(status().isBadRequest());
		transfer(aliceSession, Map.of("fromCurrency", "XXX1", "toUsername", bob, "amount", "1"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void crossCurrencyTransferUsesOfficialRate() throws Exception {
		openAccount(bobSession, "USD");

		transfer(aliceSession, Map.of(
						"fromCurrency", "UAH", "toUsername", bob,
						"toCurrency", "USD", "amount", "400"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.credit.amount").value(10.00))
				.andExpect(jsonPath("$.credit.currency").value("USD"))
				.andExpect(jsonPath("$.exchangeRate").value(0.025));

		assertThat(balance(aliceSession, "UAH")).isEqualByComparingTo("600.00");
		assertThat(balance(bobSession, "USD")).isEqualByComparingTo("10.00");
	}

	@Test
	void transferToMissingCurrencyAccountFails() throws Exception {
		transfer(aliceSession, Map.of(
						"fromCurrency", "UAH", "toUsername", bob,
						"toCurrency", "EUR", "amount", "50"))
				.andExpect(status().isNotFound());
	}

	@Test
	void exchangeBetweenOwnAccounts() throws Exception {
		openAccount(aliceSession, "EUR");

		transfer(aliceSession, Map.of(
						"fromCurrency", "UAH", "toUsername", alice,
						"toCurrency", "EUR", "amount", "500"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("EXCHANGE"));

		assertThat(balance(aliceSession, "UAH")).isEqualByComparingTo("500.00");
		assertThat(balance(aliceSession, "EUR")).isEqualByComparingTo("10.00");
	}

	@Test
	void transferToSameAccountIsRejected() throws Exception {
		transfer(aliceSession, Map.of("fromCurrency", "UAH", "toUsername", alice, "amount", "1"))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void blockingTakesEffectImmediatelyForOpenSessions() throws Exception {
		long aliceId = userId(alice);
		mvc.perform(patch("/api/admin/users/" + aliceId + "/status").session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("status", "BLOCKED"))))
				.andExpect(status().isOk());

		transfer(aliceSession, Map.of("fromCurrency", "UAH", "toUsername", bob, "amount", "1"))
				.andExpect(status().isForbidden());
		transfer(bobSession, Map.of("fromCurrency", "UAH", "toUsername", alice, "amount", "1"))
				.andExpect(status().isUnprocessableContent());
		mvc.perform(postJson("/api/auth/login", Map.of("username", alice, "password", PASSWORD)))
				.andExpect(status().isForbidden());
	}

	@Test
	void reversalRestoresBalancesAndCannotBeRepeated() throws Exception {
		long txId = readJson(transfer(aliceSession,
				Map.of("fromCurrency", "UAH", "toUsername", bob, "amount", "300")))
				.get("id").asLong();

		mvc.perform(post("/api/admin/transactions/" + txId + "/reverse").session(admin))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("REVERSAL"))
				.andExpect(jsonPath("$.reversalOfId").value(txId))
				.andExpect(jsonPath("$.fromUsername").value(bob))
				.andExpect(jsonPath("$.toUsername").value(alice));

		assertThat(balance(aliceSession, "UAH")).isEqualByComparingTo("1000.00");
		assertThat(balance(bobSession, "UAH")).isEqualByComparingTo("0");
		mvc.perform(post("/api/admin/transactions/" + txId + "/reverse").session(admin))
				.andExpect(status().isConflict());
	}

	@Test
	void reversalFailsWhenMoneyWasAlreadySpent() throws Exception {
		long txId = readJson(transfer(aliceSession,
				Map.of("fromCurrency", "UAH", "toUsername", bob, "amount", "300")))
				.get("id").asLong();
		String carol = uniqueUsername("carol");
		register(carol);
		transfer(bobSession, Map.of("fromCurrency", "UAH", "toUsername", carol, "amount", "300"))
				.andExpect(status().isCreated());

		mvc.perform(post("/api/admin/transactions/" + txId + "/reverse").session(admin))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void historyAndCsvStatementShowUserTransactions() throws Exception {
		transfer(aliceSession, Map.of("fromCurrency", "UAH", "toUsername", bob, "amount", "100"))
				.andExpect(status().isCreated());

		mvc.perform(get("/api/transactions").session(bobSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].fromUsername").value(alice));
		mvc.perform(get("/api/transactions").param("from", "2000-01-01").param("to", "2000-01-31")
						.session(bobSession))
				.andExpect(jsonPath("$.length()").value(0));

		String csv = mvc.perform(export(aliceSession, "csv"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/csv"))
				.andReturn().getResponse().getContentAsString();
		String[] lines = csv.split("\r\n");
		assertThat(lines).hasSize(3); // header + deposit + transfer
		assertThat(lines[0]).startsWith("id,created_at,type");
		assertThat(csv).contains(",TRANSFER,", ",DEPOSIT,", "100.00,UAH");
	}

	@Test
	void csvStatementContainsDescription() throws Exception {
		Map<String, String> body = new HashMap<>(uah(bob, "5"));
		body.put("description", "Подарунок, з днем народження");
		transfer(aliceSession, body).andExpect(status().isCreated());

		String csv = mvc.perform(export(aliceSession, "csv"))
				.andReturn().getResponse().getContentAsString();
		assertThat(csv.split("\r\n")[0]).contains("description");
		assertThat(csv).contains("\"Подарунок, з днем народження\"");
	}

	@Test
	void jsonStatementIsDownloadableAsAttachment() throws Exception {
		transfer(aliceSession, uah(bob, "100")).andExpect(status().isCreated());

		mvc.perform(export(aliceSession, "json"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(header().string("Content-Disposition", containsString(".json")))
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].type").value("TRANSFER"));
	}

	@Test
	void unsupportedStatementFormatIsRejected() throws Exception {
		mvc.perform(export(aliceSession, "xml")).andExpect(status().isBadRequest());
	}

	@Test
	void usersCannotSeeOtherUsersAdminData() throws Exception {
		mvc.perform(post("/api/admin/accounts/" + accountId(bobSession, "UAH") + "/deposit")
						.session(aliceSession).contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("amount", "1000000"))))
				.andExpect(status().isForbidden());
	}

	// ---- helpers ----

	/** Same-currency UAH transfer body. */
	private static Map<String, String> uah(String toUsername, String amount) {
		return Map.of("fromCurrency", "UAH", "toUsername", toUsername, "amount", amount);
	}

	private ResultActions transfer(
			MockHttpSession session, Map<String, String> body) throws Exception {
		return mvc.perform(postJson("/api/transfers", new HashMap<>(body)).session(session));
	}

	private static MockHttpServletRequestBuilder export(MockHttpSession session, String format) {
		return get("/api/transactions/export").param("format", format).session(session);
	}

	private void deposit(long accountId, String amount) throws Exception {
		mvc.perform(post("/api/admin/accounts/" + accountId + "/deposit").session(admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json(Map.of("amount", amount))))
				.andExpect(status().isCreated());
	}

	private void openAccount(MockHttpSession session, String currency) throws Exception {
		mvc.perform(postJson("/api/accounts", Map.of("currency", currency)).session(session))
				.andExpect(status().isCreated());
	}

	private JsonNode account(MockHttpSession session, String currency) throws Exception {
		JsonNode accounts = readJson(mvc.perform(get("/api/accounts").session(session)));
		for (JsonNode account : accounts) {
			if (account.get("balance").get("currency").asString().equals(currency)) {
				return account;
			}
		}
		throw new AssertionError("No " + currency + " account");
	}

	private long accountId(MockHttpSession session, String currency) throws Exception {
		return account(session, currency).get("id").asLong();
	}

	private BigDecimal balance(MockHttpSession session, String currency) throws Exception {
		return account(session, currency).get("balance").get("amount").decimalValue();
	}

	private long userId(String username) throws Exception {
		for (JsonNode user : readJson(mvc.perform(get("/api/admin/users").session(admin)))) {
			if (user.get("username").asString().equals(username)) {
				return user.get("id").asLong();
			}
		}
		throw new AssertionError("No user " + username);
	}

	private JsonNode readJson(ResultActions result) throws Exception {
		return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString());
	}
}
