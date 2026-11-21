package io.github.lucyechkaaa.jfinance.user;

import io.github.lucyechkaaa.jfinance.account.AccountService;
import io.github.lucyechkaaa.jfinance.audit.AuditAction;
import io.github.lucyechkaaa.jfinance.audit.AuditEntry;
import io.github.lucyechkaaa.jfinance.audit.AuditService;
import io.github.lucyechkaaa.jfinance.common.exception.ConflictException;
import io.github.lucyechkaaa.jfinance.common.exception.ForbiddenOperationException;
import io.github.lucyechkaaa.jfinance.common.exception.NotFoundException;
import java.time.Clock;
import java.util.Locale;
import java.util.Objects;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** User registration and lookup. */
@Service
public class UserService {

	private final UserRepository userRepository;
	private final AccountService accountService;
	private final PasswordEncoder passwordEncoder;
	private final AuditService auditService;
	private final Clock clock;

	public UserService(
			UserRepository userRepository,
			AccountService accountService,
			PasswordEncoder passwordEncoder,
			AuditService auditService,
			Clock clock) {
		this.userRepository = userRepository;
		this.accountService = accountService;
		this.passwordEncoder = passwordEncoder;
		this.auditService = auditService;
		this.clock = clock;
	}

	/**
	 * Creates a user with the given role and opens an account in the base currency.
	 *
	 * @throws ConflictException if the username is taken (case-insensitive)
	 */
	@Transactional
	public User register(String rawUsername, String rawPassword, Role role) {
		String username = normalize(rawUsername);
		if (userRepository.existsByUsername(username)) {
			throw new ConflictException("Username '%s' is already taken".formatted(username));
		}
		String passwordHash = Objects.requireNonNull(
				passwordEncoder.encode(rawPassword), "password encoder returned null");
		User user = userRepository.save(new User(username, passwordHash, role, clock.instant()));
		accountService.openBaseAccount(user);
		auditService.record(AuditEntry.builder()
				.actor(username)
				.action(AuditAction.REGISTER)
				.target(username)
				.details("role=" + role)
				.succeeded());
		return user;
	}

	/**
	 * User by username (case-insensitive).
	 *
	 * @throws NotFoundException if no such user exists
	 */
	@Transactional(readOnly = true)
	public User getByUsername(String rawUsername) {
		String username = normalize(rawUsername);
		return userRepository.findByUsername(username)
				.orElseThrow(() ->
						new NotFoundException("User '%s' not found".formatted(username)));
	}

	/**
	 * Returns the user only if not blocked. Used for every money operation so that blocking takes
	 * effect immediately, even for already open sessions.
	 */
	@Transactional(readOnly = true)
	public User getActiveUser(String username) {
		User user = getByUsername(username);
		if (!user.isActive()) {
			throw new ForbiddenOperationException(
					"User '%s' is blocked".formatted(user.getUsername()));
		}
		return user;
	}

	/** Canonical username form: trimmed and lower-case, so lookups are case-insensitive. */
	public static String normalize(String username) {
		return username.trim().toLowerCase(Locale.ROOT);
	}
}
