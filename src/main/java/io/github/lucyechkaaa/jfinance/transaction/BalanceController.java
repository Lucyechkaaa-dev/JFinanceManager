package io.github.lucyechkaaa.jfinance.transaction;

import io.github.lucyechkaaa.jfinance.account.Account;
import io.github.lucyechkaaa.jfinance.account.AccountService;
import io.github.lucyechkaaa.jfinance.common.exception.ForbiddenOperationException;
import io.github.lucyechkaaa.jfinance.transaction.dto.BalanceReport;
import io.github.lucyechkaaa.jfinance.user.User;
import io.github.lucyechkaaa.jfinance.user.UserService;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Account balance recalculated from the ledger (FR-7). */
@RestController
public class BalanceController {

	private final AccountService accountService;
	private final UserService userService;
	private final LedgerBalanceCalculator calculator;

	public BalanceController(
			AccountService accountService,
			UserService userService,
			LedgerBalanceCalculator calculator) {
		this.accountService = accountService;
		this.userService = userService;
		this.calculator = calculator;
	}

	/** Balance check for one of the current user's own accounts. */
	@GetMapping("/api/accounts/{accountId}/balance")
	public BalanceReport ownBalance(@PathVariable long accountId, Principal principal) {
		User user = userService.getByUsername(principal.getName());
		Account account = accountService.getById(accountId);
		if (!account.getOwner().getId().equals(user.getId())) {
			throw new ForbiddenOperationException("Account %d is not yours".formatted(accountId));
		}
		return report(account);
	}

	/** Balance check for any account (administrators). */
	@GetMapping("/api/admin/accounts/{accountId}/balance")
	public BalanceReport anyBalance(@PathVariable long accountId) {
		return report(accountService.getById(accountId));
	}

	private BalanceReport report(Account account) {
		return BalanceReport.of(account.getId(), account.getBalance(), calculator.calculate(account));
	}
}
