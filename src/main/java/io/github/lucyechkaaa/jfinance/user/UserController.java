package io.github.lucyechkaaa.jfinance.user;

import io.github.lucyechkaaa.jfinance.user.dto.UserResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Current-user endpoints. */
@RestController
@RequestMapping("/api/me")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	/** Profile of the authenticated user. */
	@GetMapping
	public UserResponse me(Principal principal) {
		return UserResponse.from(userService.getByUsername(principal.getName()));
	}
}
