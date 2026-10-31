package io.github.lucyechkaaa.jfinance.admin.dto;

import io.github.lucyechkaaa.jfinance.user.Role;
import io.github.lucyechkaaa.jfinance.user.UserStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/** Request bodies of the admin API. */
public final class AdminRequests {

	private AdminRequests() {
	}

	/** New status for a user. */
	public record ChangeStatusRequest(@NotNull UserStatus status) {
	}

	/** New role for a user. */
	public record ChangeRoleRequest(@NotNull Role role) {
	}

	/** Amount in the account's own currency. */
	public record DepositRequest(
			@NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal amount) {
	}
}
