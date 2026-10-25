package io.github.lucyechkaaa.jfinance.auth;

import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Authenticates credentials and applies brute-force protection. Deliberately not
 * transactional: the failure counter must be committed even though authentication fails.
 */
@Service
public class LoginService {

	private final AuthenticationManager authenticationManager;
	private final LoginAttemptService loginAttemptService;

	public LoginService(
			AuthenticationManager authenticationManager, LoginAttemptService loginAttemptService) {
		this.authenticationManager = authenticationManager;
		this.loginAttemptService = loginAttemptService;
	}

	/**
	 * Verifies the credentials and returns the authenticated principal.
	 *
	 * @throws BadCredentialsException wrong username or password
	 * @throws LockedException         too many failed attempts, temporarily locked
	 * @throws DisabledException       account blocked by an administrator
	 */
	public Authentication login(String rawUsername, String password) {
		String username = rawUsername.toLowerCase(Locale.ROOT);
		try {
			Authentication authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(username, password));
			loginAttemptService.onSuccess(username);
			return authentication;
		} catch (BadCredentialsException ex) {
			loginAttemptService.onBadCredentials(username);
			throw ex;
		} catch (LockedException ex) {
			loginAttemptService.onRejected(username, "account temporarily locked");
			throw ex;
		} catch (DisabledException ex) {
			loginAttemptService.onRejected(username, "account blocked");
			throw ex;
		}
	}
}
