package io.github.lucyechkaaa.jfinance.transaction.export;

import io.github.lucyechkaaa.jfinance.transaction.dto.TransactionView;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Statement as a JSON array of transactions. */
@Component
public class JsonStatementExporter implements StatementExporter {

	public JsonStatementExporter(JsonMapper jsonMapper) {
		// not implemented yet
	}

	@Override
	public String format() {
		throw new UnsupportedOperationException("not implemented yet");
	}

	@Override
	public MediaType contentType() {
		throw new UnsupportedOperationException("not implemented yet");
	}

	@Override
	public String export(List<TransactionView> transactions) {
		throw new UnsupportedOperationException("not implemented yet");
	}
}
