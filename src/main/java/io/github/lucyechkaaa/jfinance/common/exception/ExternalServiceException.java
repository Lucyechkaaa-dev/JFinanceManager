package io.github.lucyechkaaa.jfinance.common.exception;

/** A required external system (e.g. NBU rates API) is unavailable (HTTP 503). */
public class ExternalServiceException extends RuntimeException {

	public ExternalServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
