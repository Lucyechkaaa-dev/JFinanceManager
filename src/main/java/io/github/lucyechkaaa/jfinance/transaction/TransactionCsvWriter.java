package io.github.lucyechkaaa.jfinance.transaction;

import io.github.lucyechkaaa.jfinance.common.money.Money;
import io.github.lucyechkaaa.jfinance.transaction.dto.TransactionView;
import io.github.lucyechkaaa.jfinance.transaction.export.StatementExporter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Renders transactions as RFC 4180 CSV. Values starting with formula characters are prefixed
 * with an apostrophe to prevent CSV/formula injection when opened in spreadsheet software.
 */
@Component
public class TransactionCsvWriter implements StatementExporter {

	private static final String HEADER = String.join(",",
			"id", "created_at", "type",
			"from_user", "from_account", "debit_amount", "debit_currency",
			"to_user", "to_account", "credit_amount", "credit_currency",
			"exchange_rate", "description", "reversal_of", "initiated_by");
	private static final String LINE_END = "\r\n";
	private static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

	@Override
	public String format() {
		return "csv";
	}

	@Override
	public MediaType contentType() {
		return TEXT_CSV;
	}

	@Override
	public String export(List<TransactionView> transactions) {
		return write(transactions);
	}

	/** Renders a header row followed by one row per transaction, CRLF-separated. */
	public String write(List<TransactionView> transactions) {
		StringBuilder csv = new StringBuilder(HEADER).append(LINE_END);
		for (TransactionView tx : transactions) {
			csv.append(row(tx)).append(LINE_END);
		}
		return csv.toString();
	}

	private static String row(TransactionView tx) {
		return Stream.of(
						tx.id(), tx.createdAt(), tx.type(),
						tx.fromUsername(), tx.fromAccountId(),
						amount(tx.debit()), currency(tx.debit()),
						tx.toUsername(), tx.toAccountId(),
						amount(tx.credit()), currency(tx.credit()),
						plain(tx.exchangeRate()), tx.description(), tx.reversalOfId(), tx.initiatedBy())
				.map(value -> value == null ? "" : escape(value.toString()))
				.collect(Collectors.joining(","));
	}

	private static String amount(Money money) {
		return money == null ? null : money.amount().toPlainString();
	}

	private static String currency(Money money) {
		return money == null ? null : money.currency().getCurrencyCode();
	}

	private static String plain(BigDecimal value) {
		return value == null ? null : value.toPlainString();
	}

	static String escape(String value) {
		String safe = Objects.requireNonNull(value);
		if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0 && !isNumber(safe)) {
			safe = "'" + safe;
		}
		if (safe.chars().anyMatch(ch -> ch == ',' || ch == '"' || ch == '\n' || ch == '\r')) {
			safe = "\"" + safe.replace("\"", "\"\"") + "\"";
		}
		return safe;
	}

	private static boolean isNumber(String value) {
		try {
			new BigDecimal(value);
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}
}
