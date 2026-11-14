package io.github.lucyechkaaa.jfinance.transaction.export;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lucyechkaaa.jfinance.common.money.Money;
import io.github.lucyechkaaa.jfinance.transaction.TransactionType;
import io.github.lucyechkaaa.jfinance.transaction.dto.TransactionView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class JsonStatementExporterTest {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final JsonStatementExporter exporter = new JsonStatementExporter(jsonMapper);

	@Test
	void describesItsFormat() {
		assertThat(exporter.format()).isEqualTo("json");
		assertThat(exporter.contentType().isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
	}

	@Test
	void emptyStatementIsAnEmptyJsonArray() {
		assertThat(jsonMapper.readTree(exporter.export(List.of())).isArray()).isTrue();
		assertThat(jsonMapper.readTree(exporter.export(List.of())).size()).isZero();
	}

	@Test
	void exportedValuesAreReadBackUnchanged() {
		TransactionView transfer = new TransactionView(7L, TransactionType.TRANSFER,
				1L, "alice", Money.of("400.00", "UAH"), 2L, "bob", Money.of("9.99", "USD"),
				new BigDecimal("0.0221698"), "Оплата \"за\" квартиру", null, "alice",
				Instant.parse("2026-11-26T10:15:30Z"));
		TransactionView deposit = new TransactionView(8L, TransactionType.DEPOSIT,
				null, null, null, 1L, "alice", Money.of("0.01", "UAH"),
				null, null, null, "admin", Instant.parse("2026-11-27T00:00:00Z"));

		JsonNode json = jsonMapper.readTree(exporter.export(List.of(transfer, deposit)));

		assertThat(json.size()).isEqualTo(2);
		JsonNode first = json.get(0);
		assertThat(first.get("id").asLong()).isEqualTo(7);
		assertThat(first.get("type").asString()).isEqualTo("TRANSFER");
		assertThat(first.get("debit").get("amount").decimalValue()).isEqualByComparingTo("400.00");
		assertThat(first.get("credit").get("currency").asString()).isEqualTo("USD");
		assertThat(first.get("exchangeRate").decimalValue()).isEqualByComparingTo("0.0221698");
		assertThat(first.get("description").asString()).isEqualTo("Оплата \"за\" квартиру");
		assertThat(first.get("createdAt").asString()).isEqualTo("2026-11-26T10:15:30Z");
		JsonNode second = json.get(1);
		assertThat(second.get("debit").isNull()).isTrue();
		assertThat(second.get("credit").get("amount").decimalValue()).isEqualByComparingTo("0.01");
	}
}
