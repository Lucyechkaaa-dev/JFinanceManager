package io.github.lucyechkaaa.jfinance.transaction.export;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lucyechkaaa.jfinance.common.money.Money;
import io.github.lucyechkaaa.jfinance.transaction.TransactionType;
import io.github.lucyechkaaa.jfinance.transaction.dto.TransactionView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvStatementExporterTest {

	@Test
	void quotesValuesWithSeparatorsAndQuotes() {
		assertThat(CsvStatementExporter.escape("a,b")).isEqualTo("\"a,b\"");
		assertThat(CsvStatementExporter.escape("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
	}

	@Test
	void neutralisesFormulaInjectionButKeepsNumbers() {
		assertThat(CsvStatementExporter.escape("=HYPERLINK(1)")).isEqualTo("'=HYPERLINK(1)");
		assertThat(CsvStatementExporter.escape("-evil")).isEqualTo("'-evil");
		assertThat(CsvStatementExporter.escape("-12.50")).isEqualTo("-12.50");
		assertThat(CsvStatementExporter.escape("plain")).isEqualTo("plain");
	}

	@Test
	void emptyStatementContainsOnlyTheHeader() {
		String csv = new CsvStatementExporter().export(List.of());

		assertThat(csv.split("\r\n")).hasSize(1);
		assertThat(csv).startsWith("id,created_at,type").endsWith("\r\n");
	}

	@Test
	void emptyStringStaysEmpty() {
		assertThat(CsvStatementExporter.escape("")).isEmpty();
	}

	@Test
	void rendersEveryColumnOfTransaction() {
		TransactionView exchange = new TransactionView(7L, TransactionType.EXCHANGE,
				1L, "alice", Money.of("400", "UAH"), 2L, "alice", Money.of("9.99", "USD"),
				new BigDecimal("0.025"), "на відпустку", null, "alice",
				Instant.parse("2026-11-26T10:15:30Z"));

		String[] lines = new CsvStatementExporter().export(List.of(exchange)).split("\r\n");

		assertThat(lines).hasSize(2);
		assertThat(lines[1]).isEqualTo("7,2026-11-26T10:15:30Z,EXCHANGE,alice,1,400.00,UAH,"
				+ "alice,2,9.99,USD,0.025,на відпустку,,alice");
	}

	@Test
	void missingSidesBecomeEmptyCells() {
		TransactionView deposit = new TransactionView(8L, TransactionType.DEPOSIT,
				null, null, null, 3L, "bob", Money.of("0.01", "UAH"),
				null, null, null, "admin", Instant.parse("2026-11-27T00:00:00Z"));

		String row = new CsvStatementExporter().export(List.of(deposit)).split("\r\n")[1];

		assertThat(row).isEqualTo("8,2026-11-27T00:00:00Z,DEPOSIT,,,,,bob,3,0.01,UAH,,,,admin");
	}

	@Test
	void describesItsFormat() {
		CsvStatementExporter exporter = new CsvStatementExporter();

		assertThat(exporter.format()).isEqualTo("csv");
		assertThat(exporter.contentType().toString()).startsWith("text/csv");
	}
}
