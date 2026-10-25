package io.github.lucyechkaaa.jfinance.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Login payload. */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {

	@Override
	public String toString() {
		return "LoginRequest[username=" + username + ", password=***]";
	}
}
