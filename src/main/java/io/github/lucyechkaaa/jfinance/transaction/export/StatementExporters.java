package io.github.lucyechkaaa.jfinance.transaction.export;

import io.github.lucyechkaaa.jfinance.common.exception.BadRequestException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

/** Chooses the {@link StatementExporter} strategy for a requested format. */
@Component
public final class StatementExporters {

	private final Map<String, StatementExporter> byFormat = new TreeMap<>();

	/**
	 * Registers every exporter bean.
	 *
	 * @throws IllegalStateException if two exporters claim the same format
	 */
	public StatementExporters(List<StatementExporter> exporters) {
		for (StatementExporter exporter : exporters) {
			String format = normalize(exporter.format());
			if (byFormat.putIfAbsent(format, exporter) != null) {
				throw new IllegalStateException(
						"Duplicate statement exporter for format " + format);
			}
		}
	}

	/**
	 * Exporter registered for {@code format} (case-insensitive, surrounding spaces ignored).
	 *
	 * @throws BadRequestException if the format is missing or unsupported
	 */
	public StatementExporter forFormat(String format) {
		StatementExporter exporter = format == null ? null : byFormat.get(normalize(format));
		if (exporter == null) {
			throw new BadRequestException("Unsupported statement format '%s'; supported: %s"
					.formatted(format, String.join(", ", byFormat.keySet())));
		}
		return exporter;
	}

	private static String normalize(String format) {
		return format.trim().toLowerCase(Locale.ROOT);
	}
}
