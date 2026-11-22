package io.github.lucyechkaaa.jfinance.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Registration payload with the basic password policy. */
public record RegisterRequest(
		@NotBlank
		@Pattern(regexp = USERNAME_PATTERN,
				message = "must be " + MIN_USERNAME_LENGTH + "-" + MAX_USERNAME_LENGTH
						+ " characters: letters, digits, '_', '.', '-'")
		String username,

		@NotNull
		@Size(min = MIN_PASSWORD_LENGTH, max = MAX_PASSWORD_LENGTH)
		@Pattern(regexp = PASSWORD_PATTERN,
				message = "must contain at least one letter and one digit")
		String password) {

	public static final int MIN_USERNAME_LENGTH = 3;
	public static final int MAX_USERNAME_LENGTH = 32;
	public static final int MIN_PASSWORD_LENGTH = 8;
	public static final int MAX_PASSWORD_LENGTH = 128;

	private static final String USERNAME_PATTERN =
			"^[A-Za-z0-9_.-]{" + MIN_USERNAME_LENGTH + "," + MAX_USERNAME_LENGTH + "}$";
	private static final String PASSWORD_PATTERN = "^(?=.*\\p{L})(?=.*\\d).+$";

	@Override
	public String toString() {
		return "RegisterRequest[username=" + username + ", password=***]";
	}
}
