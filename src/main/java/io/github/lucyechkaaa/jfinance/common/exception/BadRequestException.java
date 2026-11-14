package io.github.lucyechkaaa.jfinance.common.exception;

/** Request parameter has an unsupported or malformed value (HTTP 400). */
public class BadRequestException extends DomainException {

	public BadRequestException(String message) {
		super(message);
	}
}
