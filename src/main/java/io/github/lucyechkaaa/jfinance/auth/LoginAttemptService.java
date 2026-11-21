package io.github.lucyechkaaa.jfinance.auth;

import io.github.lucyechkaaa.jfinance.audit.AuditAction;
import io.github.lucyechkaaa.jfinance.audit.AuditEntry;
import io.github.lucyechkaaa.jfinance.audit.AuditService;
import io.github.lucyechkaaa.jfinance.user.User;
import io.github.lucyechkaaa.jfinance.user.UserRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tracks failed logins and locks accounts after too many consecutive failures. */
@Service
public class LoginAttemptService {

	private static final String BAD_CREDENTIALS = "bad credentials";

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
		auditLoginFailure(username, BAD_CREDENTIALS);
		userRepository.findByUsername(username).ifPresent(this::registerFailure);
	}

	/** Login refused for a locked or blocked account: audited, but not counted again. */
	@Transactional
	public void onRejected(String username, String reason) {
		auditLoginFailure(username, reason);
	}

	/** Successful login: resets the failure counter and audits the login. */
	@Transactional
	public void onSuccess(String username) {
		userRepository.findByUsername(username).ifPresent(User::registerSuccessfulLogin);
		auditService.record(AuditEntry.builder()
				.actor(username)
				.action(AuditAction.LOGIN)
				.target(username)
				.succeeded());
	}

	private void auditLoginFailure(String username, String reason) {
		auditService.record(AuditEntry.builder()
				.actor(username)
				.action(AuditAction.LOGIN_FAILED)
				.target(username)
				.details(reason)
				.failed());
	}

	private void registerFailure(User user) {
		boolean locked = user.registerFailedLogin(lockoutProperties.maxFailedAttempts(),
				lockoutProperties.lockDuration(), clock.instant());
		if (locked) {
			auditService.record(AuditEntry.builder()
					.actor(AuditService.SYSTEM_ACTOR)
					.action(AuditAction.ACCOUNT_LOCKED)
					.target(user.getUsername())
					.details("locked until " + user.getLockedUntil())
					.succeeded());
		}
	}
}
