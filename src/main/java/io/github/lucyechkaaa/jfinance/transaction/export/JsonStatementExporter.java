package io.github.lucyechkaaa.jfinance.transaction.export;

import io.github.lucyechkaaa.jfinance.transaction.dto.TransactionView;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Statement as a JSON array of transactions. */
@Component
public class JsonStatementExporter implements StatementExporter {

	private static final String FORMAT = "json";

	private final JsonMapper jsonMapper;

	public JsonStatementExporter(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public String format() {
		return FORMAT;
	}

	@Override
	public MediaType contentType() {
		return MediaType.APPLICATION_JSON;
	}

	@Override
	public String export(List<TransactionView> transactions) {
		return jsonMapper.writeValueAsString(transactions);
	}
}
