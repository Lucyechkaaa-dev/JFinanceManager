package io.github.lucyechkaaa.jfinance.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * System user. Holds credentials, role and login-lockout state.
 *
 * <p>State changes go through intention-revealing methods instead of setters, so invariants
 * (e.g. lockout rules) live next to the data they protect.
 */
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	// SQLite AUTOINCREMENT requires the exact column type INTEGER.
	@Column(columnDefinition = "INTEGER")
	private Long id;

	@Column(nullable = false, unique = true, length = 32)
	private String username;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private UserStatus status;

	@Column(name = "failed_login_attempts", nullable = false)
	private int failedLoginAttempts;

	@Column(name = "locked_until")
	private Instant lockedUntil;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Version
	private long version;

	protected User() {
		// for JPA
	}

	public User(String username, String passwordHash, Role role, Instant createdAt) {
		this.username = Objects.requireNonNull(username);
		this.passwordHash = Objects.requireNonNull(passwordHash);
		this.role = Objects.requireNonNull(role);
		this.status = UserStatus.ACTIVE;
		this.createdAt = Objects.requireNonNull(createdAt);
	}

	/** Whether a login lock from too many failed attempts is still active at {@code now}. */
	public boolean isLockedAt(Instant now) {
		return lockedUntil != null && now.isBefore(lockedUntil);
	}

	/** Whether the user is not blocked by an administrator. */
	public boolean isActive() {
		return status == UserStatus.ACTIVE;
	}

	/**
	 * Registers a failed login. When {@code maxAttempts} is reached the account is locked for
	 * {@code lockDuration} and the counter is reset.
	 *
	 * @return {@code true} if this failure caused a lock
	 */
	public boolean registerFailedLogin(int maxAttempts, Duration lockDuration, Instant now) {
		failedLoginAttempts++;
		if (failedLoginAttempts >= maxAttempts) {
			lockedUntil = now.plus(lockDuration);
			failedLoginAttempts = 0;
			return true;
		}
		return false;
	}

	/** Resets the failed-login counter and removes any lock. */
	public void registerSuccessfulLogin() {
		failedLoginAttempts = 0;
		lockedUntil = null;
	}

	/** Blocks or unblocks the user. */
	public void changeStatus(UserStatus newStatus) {
		this.status = Objects.requireNonNull(newStatus);
	}

	/** Grants a new role. */
	public void changeRole(Role newRole) {
		this.role = Objects.requireNonNull(newRole);
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Role getRole() {
		return role;
	}

	public UserStatus getStatus() {
		return status;
	}

	public int getFailedLoginAttempts() {
		return failedLoginAttempts;
	}

	public Instant getLockedUntil() {
		return lockedUntil;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
