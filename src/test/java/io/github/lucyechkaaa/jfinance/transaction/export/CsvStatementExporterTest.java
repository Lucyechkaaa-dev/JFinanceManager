package io.github.lucyechkaaa.jfinance.transaction.export;

import static org.assertj.core.api.Assertions.assertThat;

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
}
