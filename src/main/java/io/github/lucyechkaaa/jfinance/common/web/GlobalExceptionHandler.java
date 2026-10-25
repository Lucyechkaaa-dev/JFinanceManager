package io.github.lucyechkaaa.jfinance.common.web;

import io.github.lucyechkaaa.jfinance.common.exception.BusinessRuleException;
import io.github.lucyechkaaa.jfinance.common.exception.ConflictException;
import io.github.lucyechkaaa.jfinance.common.exception.ExternalServiceException;
import io.github.lucyechkaaa.jfinance.common.exception.ForbiddenOperationException;
import io.github.lucyechkaaa.jfinance.common.exception.NotFoundException;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Maps exceptions to RFC 9457 problem-details responses. Stack traces never leak to clients. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail handleNotFound(NotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail handleConflict(ConflictException ex) {
		return problem(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(BusinessRuleException.class)
	ProblemDetail handleBusinessRule(BusinessRuleException ex) {
		return problem(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
	}

	@ExceptionHandler(ForbiddenOperationException.class)
	ProblemDetail handleForbidden(ForbiddenOperationException ex) {
		return problem(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
		return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(BadCredentialsException.class)
	ProblemDetail handleBadCredentials(BadCredentialsException ex) {
		return problem(HttpStatus.UNAUTHORIZED, "Invalid username or password");
	}

	@ExceptionHandler(LockedException.class)
	ProblemDetail handleLocked(LockedException ex) {
		return problem(HttpStatus.LOCKED,
				"Account is temporarily locked after too many failed logins");
	}

	@ExceptionHandler(DisabledException.class)
	ProblemDetail handleDisabled(DisabledException ex) {
		return problem(HttpStatus.FORBIDDEN, "Account is blocked by an administrator");
	}

	@ExceptionHandler({
		OptimisticLockingFailureException.class, DataIntegrityViolationException.class
	})
	ProblemDetail handleConcurrentModification(RuntimeException ex) {
		log.warn("Concurrent modification or constraint violation", ex);
		return problem(HttpStatus.CONFLICT,
				"Data was modified concurrently or violates a constraint; retry");
	}

	@ExceptionHandler(ExternalServiceException.class)
	ProblemDetail handleExternal(ExternalServiceException ex) {
		log.error("External service failure", ex);
		return problem(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex) {
		log.error("Unexpected error", ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
				.collect(Collectors.toMap(FieldError::getField,
						error -> String.valueOf(error.getDefaultMessage()),
						(first, second) -> first + "; " + second));
		ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Validation failed");
		body.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(body);
	}

	private static ProblemDetail problem(HttpStatus status, String detail) {
		if (status.is4xxClientError()) {
			log.debug("Request rejected: {} {}", status.value(), detail);
		}
		return ProblemDetail.forStatusAndDetail(status, detail);
	}
}
