package io.github.lucyechkaaa.jfinance.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Registration payload with the basic password policy. */
public record RegisterRequest(
		@NotBlank
		@Pattern(regexp = "^[A-Za-z0-9_.-]{3,32}$",
				message = "must be 3-32 characters: letters, digits, '_', '.', '-'")
		String username,

		@NotNull
		@Size(min = 8, max = 128)
		@Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).+$",
				message = "must contain at least one letter and one digit")
		String password) {

	@Override
	public String toString() {
		return "RegisterRequest[username=" + username + ", password=***]";
	}
}
