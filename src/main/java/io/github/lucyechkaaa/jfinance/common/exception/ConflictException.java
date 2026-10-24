package io.github.lucyechkaaa.jfinance.common.exception;

/** Request conflicts with the current state, e.g. duplicate username (HTTP 409). */
public class ConflictException extends DomainException {

	public ConflictException(String message) {
		super(message);
	}
}
