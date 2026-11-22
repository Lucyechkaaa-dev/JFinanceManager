package io.github.lucyechkaaa.jfinance.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Puts {@code requestId} and {@code username} into the logging MDC and logs one line per request
 * (method, path, status, duration). Runs after the Spring Security filter chain, so the
 * authenticated user is already known.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
	private static final String REQUEST_ID_HEADER = "X-Request-Id";
	private static final int REQUEST_ID_LENGTH = 8;
	private static final long NANOS_PER_MILLI = 1_000_000;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String requestId = UUID.randomUUID().toString().substring(0, REQUEST_ID_LENGTH);
		MDC.put("requestId", requestId);
		response.setHeader(REQUEST_ID_HEADER, requestId);
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated()
				&& !(auth instanceof AnonymousAuthenticationToken)) {
			MDC.put("username", auth.getName());
		}
		long start = System.nanoTime();
		try {
			chain.doFilter(request, response);
		} finally {
			log.info("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(),
					response.getStatus(), (System.nanoTime() - start) / NANOS_PER_MILLI);
			MDC.clear();
		}
	}
}
