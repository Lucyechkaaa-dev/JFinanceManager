package io.github.lucyechkaaa.jfinance.user.dto;

import io.github.lucyechkaaa.jfinance.user.Role;
import io.github.lucyechkaaa.jfinance.user.User;
import io.github.lucyechkaaa.jfinance.user.UserStatus;
import java.time.Instant;

/** Public view of a user. Never exposes the password hash. */
public record UserResponse(
		Long id,
		String username,
		Role role,
		UserStatus status,
		Instant lockedUntil,
		Instant createdAt) {

	/** Maps the entity to its public view. */
	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.getStatus(),
				user.getLockedUntil(), user.getCreatedAt());
	}
}
