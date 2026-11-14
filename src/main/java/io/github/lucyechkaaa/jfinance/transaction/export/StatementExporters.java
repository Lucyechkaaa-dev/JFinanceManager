package io.github.lucyechkaaa.jfinance.transaction.export;

import java.util.List;
import org.springframework.stereotype.Component;

/** Chooses the {@link StatementExporter} strategy for a requested format. */
@Component
public class StatementExporters {

	public StatementExporters(List<StatementExporter> exporters) {
		// not implemented yet
	}

	/** Exporter registered for {@code format} (case-insensitive). */
	public StatementExporter forFormat(String format) {
		throw new UnsupportedOperationException("not implemented yet");
	}
}
