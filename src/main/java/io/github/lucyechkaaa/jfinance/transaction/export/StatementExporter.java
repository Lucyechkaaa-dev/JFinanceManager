package io.github.lucyechkaaa.jfinance.transaction.export;

import io.github.lucyechkaaa.jfinance.transaction.dto.TransactionView;
import java.util.List;
import org.springframework.http.MediaType;

/**
 * Strategy: renders an account statement in one file format. A new format is added by a new
 * implementation (a Spring bean); no existing code has to change.
 */
public interface StatementExporter {

	/** Format key used in {@code ?format=...}, lower case, e.g. {@code "csv"}. */
	String format();

	/** Content type of the produced document. */
	MediaType contentType();

	/** Renders the transactions; an empty list yields a valid empty document. */
	String export(List<TransactionView> transactions);
}
