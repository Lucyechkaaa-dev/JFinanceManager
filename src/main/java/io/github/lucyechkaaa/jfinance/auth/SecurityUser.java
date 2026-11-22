package io.github.lucyechkaaa.jfinance.auth;

import io.github.lucyechkaaa.jfinance.user.User;
import java.io.Serial;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security view of a {@link User}. Kept separate from the entity so the domain model
 * does not depend on the security framework.
 */
public final class SecurityUser implements UserDetails {

	@Serial
	private static final long serialVersionUID = 1L;

	private static final String ROLE_PREFIX = "ROLE_";

	private final String username;
	private final String passwordHash;
	private final List<GrantedAuthority> authorities;
	private final boolean enabled;
	private final boolean nonLocked;

	private SecurityUser(
			String username,
			String passwordHash,
			List<GrantedAuthority> authorities,
			boolean enabled,
			boolean nonLocked) {
		this.username = username;
		this.passwordHash = passwordHash;
		this.authorities = authorities;
		this.enabled = enabled;
		this.nonLocked = nonLocked;
	}

	/** Snapshot of {@code user} with lock state evaluated at {@code now}. */
	public static SecurityUser from(User user, Instant now) {
		return new SecurityUser(
				user.getUsername(),
				user.getPasswordHash(),
				List.of(new SimpleGrantedAuthority(ROLE_PREFIX + user.getRole().name())),
				user.isActive(),
				!user.isLockedAt(now));
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public boolean isAccountNonLocked() {
		return nonLocked;
	}
}
