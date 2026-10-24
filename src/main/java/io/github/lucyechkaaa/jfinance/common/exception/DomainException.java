package io.github.lucyechkaaa.jfinance.common.exception;

/**
 * Base class for all expected business errors. Each subclass maps to one HTTP status in
 * {@code GlobalExceptionHandler}, so services never deal with HTTP concerns.
 */
public abstract class DomainException extends RuntimeException {

	protected DomainException(String message) {
		super(message);
	}
}
