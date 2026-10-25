package io.github.lucyechkaaa.jfinance.account;

import io.github.lucyechkaaa.jfinance.account.dto.AccountResponse;
import io.github.lucyechkaaa.jfinance.account.dto.OpenAccountRequest;
import io.github.lucyechkaaa.jfinance.user.User;
import io.github.lucyechkaaa.jfinance.user.UserService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Current user's currency accounts. */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

	private final AccountService accountService;
	private final UserService userService;

	public AccountController(AccountService accountService, UserService userService) {
		this.accountService = accountService;
		this.userService = userService;
	}

	/** Lists the current user's accounts with balances. */
	@GetMapping
	public List<AccountResponse> myAccounts(Principal principal) {
		User user = userService.getByUsername(principal.getName());
		return accountService.listFor(user).stream().map(AccountResponse::from).toList();
	}

	/** Opens an account in another supported currency (one per currency). */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AccountResponse open(
			@Valid @RequestBody OpenAccountRequest request, Principal principal) {
		User user = userService.getActiveUser(principal.getName());
		return AccountResponse.from(accountService.open(user, request.currency()));
	}
}
