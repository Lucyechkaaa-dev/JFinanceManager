package io.github.lucyechkaaa.jfinance.transaction.export;

import static org.assertj.core.api.Assertions.assertThat;

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
}
