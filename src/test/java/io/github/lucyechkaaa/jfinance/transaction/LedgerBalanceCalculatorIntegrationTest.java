package io.github.lucyechkaaa.jfinance.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.lucyechkaaa.jfinance.account.Account;
import io.github.lucyechkaaa.jfinance.account.AccountService;
import io.github.lucyechkaaa.jfinance.admin.AdminService;
import io.github.lucyechkaaa.jfinance.common.money.Money;
import io.github.lucyechkaaa.jfinance.support.IntegrationTest;
import io.github.lucyechkaaa.jfinance.user.Role;
import io.github.lucyechkaaa.jfinance.user.User;
import io.github.lucyechkaaa.jfinance.user.UserService;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

/** FR-7: account balance is computed from the ledger and reconciled with the stored one. */
class LedgerBalanceCalculatorIntegrationTest extends IntegrationTest {

	private static final Currency UAH = Currency.getInstance("UAH");
	private static final Currency USD = Currency.getInstance("USD");

	@Autowired
	private LedgerBalanceCalculator calculator;
	@Autowired
	private UserService userService;
	@Autowired
	private AccountService accountService;
	@Autowired
	private AdminService adminService;
	@Autowired
	private TransferService transferService;

	private User alice;
	private User bob;

	@BeforeEach
	void setUp() {
		alice = userService.register(uniqueUsername("alice"), PASSWORD, Role.USER);
		bob = userService.register(uniqueUsername("bob"), PASSWORD, Role.USER);
	}

	@Test
	void accountWithoutTransactionsHasZeroBalance() {
		assertThat(calculator.calculate(uahAccount(bob))).isEqualTo(Money.zero(UAH));
	}

	@Test
	void sumsDepositsAndTransfersInBothDirections() {
		adminService.deposit(ADMIN, uahAccount(alice).getId(), new BigDecimal("100.00"));
		send(alice, bob, UAH, "30.25");
		send(bob, alice, UAH, "0.25");

		assertThat(calculator.calculate(uahAccount(alice))).isEqualTo(Money.of("70.00", "UAH"));
		assertThat(calculator.calculate(uahAccount(bob))).isEqualTo(Money.of("30.00", "UAH"));
	}

	@Test
	void spendingWholeBalanceGivesExactlyZero() {
		adminService.deposit(ADMIN, uahAccount(alice).getId(), new BigDecimal("0.01"));
		send(alice, bob, UAH, "0.01");

		assertThat(calculator.calculate(uahAccount(alice))).isEqualTo(Money.zero(UAH));
	}

	@Test
	void reversalCancelsTheOriginalInTheLedger() {
		adminService.deposit(ADMIN, uahAccount(alice).getId(), new BigDecimal("50"));
		long txId = send(alice, bob, UAH, "20").id();
		adminService.reverse(ADMIN, txId);

		assertThat(calculator.calculate(uahAccount(alice))).isEqualTo(Money.of("50", "UAH"));
		assertThat(calculator.calculate(uahAccount(bob))).isEqualTo(Money.zero(UAH));
	}

	@Test
	void crossCurrencyCreditIsCountedInTargetCurrency() {
		accountService.open(bob, USD);
		adminService.deposit(ADMIN, uahAccount(alice).getId(), new BigDecimal("400"));
		transferService.transfer(new TransferCommand(alice.getUsername(), UAH, bob.getUsername(),
				USD, new BigDecimal("400"), null));

		Account bobUsd = accountService.getFor(bob, USD);
		assertThat(calculator.calculate(bobUsd)).isEqualTo(Money.of("10.00", "USD"));
		assertThat(calculator.calculate(uahAccount(alice))).isEqualTo(Money.zero(UAH));
	}

	@Test
	void ownerSeesStoredAndCalculatedBalanceAsConsistent() throws Exception {
		adminService.deposit(ADMIN, uahAccount(alice).getId(), new BigDecimal("12.34"));
		MockHttpSession session = login(alice.getUsername(), PASSWORD);

		mvc.perform(get("/api/accounts/" + uahAccount(alice).getId() + "/balance").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.storedBalance.amount").value(12.34))
				.andExpect(jsonPath("$.ledgerBalance.amount").value(12.34))
				.andExpect(jsonPath("$.consistent").value(true));
	}

	@Test
	void otherUsersAccountBalanceIsForbidden() throws Exception {
		MockHttpSession bobSession = login(bob.getUsername(), PASSWORD);

		mvc.perform(get("/api/accounts/" + uahAccount(alice).getId() + "/balance")
						.session(bobSession))
				.andExpect(status().isForbidden());
	}

	private Account uahAccount(User user) {
		return accountService.getFor(user, UAH);
	}

	private io.github.lucyechkaaa.jfinance.transaction.dto.TransactionView send(
			User from, User to, Currency currency, String amount) {
		return transferService.transfer(new TransferCommand(from.getUsername(), currency,
				to.getUsername(), currency, new BigDecimal(amount), null));
	}
}
