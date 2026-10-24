package io.github.lucyechkaaa.jfinance.common.exception;

/** Request is well-formed but violates a business rule (HTTP 422). */
public class BusinessRuleException extends DomainException {

	public BusinessRuleException(String message) {
		super(message);
	}
}
