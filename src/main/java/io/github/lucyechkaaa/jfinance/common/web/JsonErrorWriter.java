package io.github.lucyechkaaa.jfinance.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes RFC 9457 problem-details JSON from servlet filters (security layer), where
 * {@code @RestControllerAdvice} is not involved.
 */
@Component
public class JsonErrorWriter {

	private final JsonMapper jsonMapper;

	public JsonErrorWriter(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	/** Writes a problem-details body with the given status to the response. */
	public void write(
			HttpServletRequest request,
			HttpServletResponse response,
			HttpStatus status,
			String detail) throws IOException {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("type", "about:blank");
		body.put("title", status.getReasonPhrase());
		body.put("status", status.value());
		body.put("detail", detail);
		body.put("instance", request.getRequestURI());
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		jsonMapper.writeValue(response.getOutputStream(), body);
	}
}
