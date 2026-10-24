package io.github.lucyechkaaa.jfinance.common.exception;

/** Requested resource does not exist (HTTP 404). */
public class NotFoundException extends DomainException {

	public NotFoundException(String message) {
		super(message);
	}
}
