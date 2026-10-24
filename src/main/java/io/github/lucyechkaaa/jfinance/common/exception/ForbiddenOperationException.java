package io.github.lucyechkaaa.jfinance.common.exception;

/** Authenticated user is not allowed to perform the operation (HTTP 403). */
public class ForbiddenOperationException extends DomainException {

	public ForbiddenOperationException(String message) {
		super(message);
	}
}
