package io.github.lucyechkaaa.jfinance.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lucyechkaaa.jfinance.account.AccountService;
import io.github.lucyechkaaa.jfinance.admin.AdminService;
import io.github.lucyechkaaa.jfinance.common.exception.InsufficientFundsException;
import io.github.lucyechkaaa.jfinance.common.money.Money;
import io.github.lucyechkaaa.jfinance.support.IntegrationTest;
import io.github.lucyechkaaa.jfinance.user.Role;
import io.github.lucyechkaaa.jfinance.user.User;
import io.github.lucyechkaaa.jfinance.user.UserService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ConcurrencyAndLedgerIntegrationTest extends IntegrationTest {

	private static final Currency UAH = Currency.getInstance("UAH");
	private static final int THREADS = 20;

	@Autowired
	private UserService userService;

	@Autowired
	private AccountService accountService;

	@Autowired
	private AdminService adminService;

	@Autowired
	private TransferService transferService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void parallelTransfersNeverOverdrawOrLoseMoney() throws Exception {
		User sender = userService.register(uniqueUsername("rich"), PASSWORD, Role.USER);
		User recipient = userService.register(uniqueUsername("lucky"), PASSWORD, Role.USER);
		long senderAccount = accountService.getFor(sender, UAH).getId();
		adminService.deposit(ADMIN, senderAccount, new BigDecimal("100.00"));

		// 20 concurrent transfers of 10 UAH from a balance of 100 UAH: exactly 10 may succeed.
		CountDownLatch start = new CountDownLatch(1);
		Callable<Boolean> task = () -> {
			start.await();
			try {
				transferService.transfer(sender.getUsername(), UAH, recipient.getUsername(), UAH,
						new BigDecimal("10.00"));
				return true;
			} catch (InsufficientFundsException ex) {
				return false;
			}
		};
		List<Future<Boolean>> results = new ArrayList<>();
		try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
			for (int i = 0; i < THREADS; i++) {
				results.add(pool.submit(task));
			}
			start.countDown();
			pool.shutdown();
			assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();
		}

		long succeeded = 0;
		for (Future<Boolean> result : results) {
			succeeded += result.get() ? 1 : 0;
		}
		assertThat(succeeded).isEqualTo(10);
		assertThat(accountService.getFor(sender, UAH).getBalance()).isEqualTo(Money.zero(UAH));
		assertThat(accountService.getFor(recipient, UAH).getBalance())
				.isEqualTo(Money.of("100", "UAH"));
		assertLedgerMatchesBalances();
	}

	/**
	 * Core invariant of the system: every account balance equals the sum of its ledger credits
	 * minus the sum of its ledger debits. Checked over the whole test database.
	 */
	private void assertLedgerMatchesBalances() {
		String sql =
				"""
				SELECT a.id, a.balance_minor,
				       COALESCE((SELECT SUM(credit_amount_minor) FROM transactions t
				                 WHERE t.target_account_id = a.id), 0)
				     - COALESCE((SELECT SUM(debit_amount_minor) FROM transactions t
				                 WHERE t.source_account_id = a.id), 0) AS ledger
				FROM accounts a
				""";
		List<String> mismatches = jdbcTemplate.query(sql, (rs, row) -> {
			long balance = rs.getLong("balance_minor");
			long ledger = rs.getLong("ledger");
			String mismatch = "account %d: balance %d != ledger %d";
			return balance == ledger ? null : mismatch.formatted(rs.getLong("id"), balance, ledger);
		}).stream().filter(Objects::nonNull).toList();
		assertThat(mismatches).isEmpty();
	}
}
