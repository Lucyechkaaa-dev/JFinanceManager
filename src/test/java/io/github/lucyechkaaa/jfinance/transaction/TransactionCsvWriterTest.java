package io.github.lucyechkaaa.jfinance.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TransactionCsvWriterTest {

	@Test
	void quotesValuesWithSeparatorsAndQuotes() {
		assertThat(TransactionCsvWriter.escape("a,b")).isEqualTo("\"a,b\"");
		assertThat(TransactionCsvWriter.escape("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
	}

	@Test
	void neutralisesFormulaInjectionButKeepsNumbers() {
		assertThat(TransactionCsvWriter.escape("=HYPERLINK(1)")).isEqualTo("'=HYPERLINK(1)");
		assertThat(TransactionCsvWriter.escape("-evil")).isEqualTo("'-evil");
		assertThat(TransactionCsvWriter.escape("-12.50")).isEqualTo("-12.50");
		assertThat(TransactionCsvWriter.escape("plain")).isEqualTo("plain");
	}
}
