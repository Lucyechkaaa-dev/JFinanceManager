package io.github.lucyechkaaa.jfinance.transaction.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lucyechkaaa.jfinance.common.exception.BadRequestException;
import io.github.lucyechkaaa.jfinance.transaction.dto.TransactionView;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class StatementExportersTest {

	private final StatementExporter csv = new FakeExporter("csv");
	private final StatementExporter json = new FakeExporter("json");
	private final StatementExporters exporters = new StatementExporters(List.of(csv, json));

	@Test
	void resolvesExporterByFormat() {
		assertThat(exporters.forFormat("csv")).isSameAs(csv);
		assertThat(exporters.forFormat("json")).isSameAs(json);
	}

	@Test
	void formatIsCaseInsensitiveAndTrimmed() {
		assertThat(exporters.forFormat(" JSON ")).isSameAs(json);
	}

	@Test
	void unknownFormatIsRejectedWithListOfSupportedFormats() {
		assertThatThrownBy(() -> exporters.forFormat("xml"))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("xml")
				.hasMessageContaining("csv")
				.hasMessageContaining("json");
	}

	@Test
	void emptyOrMissingFormatIsRejected() {
		assertThatThrownBy(() -> exporters.forFormat(""))
				.isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> exporters.forFormat(null))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void twoExportersForTheSameFormatAreAConfigurationError() {
		List<StatementExporter> duplicates = List.of(csv, new FakeExporter("CSV"));

		assertThatThrownBy(() -> new StatementExporters(duplicates))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("csv");
	}

	private record FakeExporter(String format) implements StatementExporter {

		@Override
		public MediaType contentType() {
			return MediaType.TEXT_PLAIN;
		}

		@Override
		public String export(List<TransactionView> transactions) {
			return format;
		}
	}
}
