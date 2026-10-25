package io.github.lucyechkaaa.jfinance.auth;

import io.github.lucyechkaaa.jfinance.audit.AuditAction;
import io.github.lucyechkaaa.jfinance.audit.AuditService;
import io.github.lucyechkaaa.jfinance.user.User;
import io.github.lucyechkaaa.jfinance.user.UserRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tracks failed logins and locks accounts after too many consecutive failures. */
@Service
public class LoginAttemptService {

	private final UserRepository userRepository;
	private final LockoutProperties lockoutProperties;
	private final AuditService auditService;
	private final Clock clock;

	public LoginAttemptService(
			UserRepository userRepository,
			LockoutProperties lockoutProperties,
			AuditService auditService,
			Clock clock) {
		this.userRepository = userRepository;
		this.lockoutProperties = lockoutProperties;
		this.auditService = auditService;
		this.clock = clock;
	}

	/** Wrong password: counts towards the lockout threshold. */
	@Transactional
	public void onBadCredentials(String username) {
		auditService.failure(username, AuditAction.LOGIN_FAILED, username, "bad credentials");
		userRepository.findByUsername(username).ifPresent(this::registerFailure);
	}

	/** Login refused for a locked or blocked account: audited, but not counted again. */
	@Transactional
	public void onRejected(String username, String reason) {
		auditService.failure(username, AuditAction.LOGIN_FAILED, username, reason);
	}

	/** Successful login: resets the failure counter and audits the login. */
	@Transactional
	public void onSuccess(String username) {
		userRepository.findByUsername(username).ifPresent(User::registerSuccessfulLogin);
		auditService.success(username, AuditAction.LOGIN, username, null);
	}

	private void registerFailure(User user) {
		boolean locked = user.registerFailedLogin(lockoutProperties.maxFailedAttempts(),
				lockoutProperties.lockDuration(), clock.instant());
		if (locked) {
			auditService.success("system", AuditAction.ACCOUNT_LOCKED, user.getUsername(),
					"locked until " + user.getLockedUntil());
		}
	}
}
