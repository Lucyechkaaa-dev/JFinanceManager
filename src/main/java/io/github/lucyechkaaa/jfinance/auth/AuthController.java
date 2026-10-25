package io.github.lucyechkaaa.jfinance.auth;

import io.github.lucyechkaaa.jfinance.auth.dto.LoginRequest;
import io.github.lucyechkaaa.jfinance.auth.dto.RegisterRequest;
import io.github.lucyechkaaa.jfinance.user.Role;
import io.github.lucyechkaaa.jfinance.user.UserService;
import io.github.lucyechkaaa.jfinance.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Registration and session login. Logout is handled by Spring Security's logout filter. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final UserService userService;
	private final LoginService loginService;
	private final SecurityContextRepository securityContextRepository;
	private final SecurityContextHolderStrategy contextHolderStrategy =
			SecurityContextHolder.getContextHolderStrategy();

	public AuthController(
			UserService userService,
			LoginService loginService,
			SecurityContextRepository securityContextRepository) {
		this.userService = userService;
		this.loginService = loginService;
		this.securityContextRepository = securityContextRepository;
	}

	/** Registers a regular user; a UAH account is opened automatically. */
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse register(@Valid @RequestBody RegisterRequest request) {
		return UserResponse.from(
				userService.register(request.username(), request.password(), Role.USER));
	}

	/** Authenticates and stores the security context in the HTTP session (JSESSIONID cookie). */
	@PostMapping("/login")
	public UserResponse login(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		Authentication authentication = loginService.login(request.username(), request.password());

		SecurityContext context = contextHolderStrategy.createEmptyContext();
		context.setAuthentication(authentication);
		contextHolderStrategy.setContext(context);
		if (httpRequest.getSession(false) != null) {
			httpRequest.changeSessionId(); // session fixation protection
		}
		securityContextRepository.saveContext(context, httpRequest, httpResponse);

		return UserResponse.from(userService.getByUsername(authentication.getName()));
	}
}
