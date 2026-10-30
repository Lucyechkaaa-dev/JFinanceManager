package io.github.lucyechkaaa.jfinance.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lucyechkaaa.jfinance.account.Account;
import io.github.lucyechkaaa.jfinance.account.AccountService;
import io.github.lucyechkaaa.jfinance.audit.AuditAction;
import io.github.lucyechkaaa.jfinance.audit.AuditService;
import io.github.lucyechkaaa.jfinance.common.exception.BusinessRuleException;
import io.github.lucyechkaaa.jfinance.common.exception.ForbiddenOperationException;
import io.github.lucyechkaaa.jfinance.common.exception.InsufficientFundsException;
import io.github.lucyechkaaa.jfinance.common.money.Money;
import io.github.lucyechkaaa.jfinance.exchange.Conversion;
import io.github.lucyechkaaa.jfinance.exchange.CurrencyConverter;
import io.github.lucyechkaaa.jfinance.transaction.dto.TransactionView;
import io.github.lucyechkaaa.jfinance.user.Role;
import io.github.lucyechkaaa.jfinance.user.User;
import io.github.lucyechkaaa.jfinance.user.UserService;
import io.github.lucyechkaaa.jfinance.user.UserStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Business rules of {@link TransferService} in isolation; collaborators are mocked. */
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

	private static final Currency UAH = Currency.getInstance("UAH");
	private static final Currency USD = Currency.getInstance("USD");
	private static final Instant NOW = Instant.parse("2026-11-26T12:00:00Z");

	@Mock
	private UserService userService;
	@Mock
	private AccountService accountService;
	@Mock
	private CurrencyConverter currencyConverter;
	@Mock
	private TransactionRepository transactionRepository;
	@Mock
	private AuditService auditService;

	private TransferService service;
	private User alice;
	private User bob;
	private Account aliceUah;
	private Account bobUah;

	@BeforeEach
	void setUp() {
		service = new TransferService(userService, accountService, currencyConverter,
				transactionRepository, auditService, Clock.fixed(NOW, ZoneOffset.UTC));
		alice = user(1L, "alice");
		bob = user(2L, "bob");
		aliceUah = account(10L, alice, UAH, "100.00");
		bobUah = account(20L, bob, UAH, "0");
	}

	@Test
	void sameCurrencyTransferDebitsCreditsAndAudits() {
		givenUsersAndAccounts(bobUah);
		when(currencyConverter.convert(Money.of("40", "UAH"), UAH))
				.thenReturn(new Conversion(Money.of("40", "UAH"), Money.of("40", "UAH"), null));
		when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		TransactionView view = service.transfer("alice", UAH, "bob", UAH, new BigDecimal("40"));

		assertThat(view.type()).isEqualTo(TransactionType.TRANSFER);
		assertThat(aliceUah.getBalance()).isEqualTo(Money.of("60", "UAH"));
		assertThat(bobUah.getBalance()).isEqualTo(Money.of("40", "UAH"));
		verify(auditService).success(eq("alice"), eq(AuditAction.TRANSFER), any(), any());
	}

	@Test
	void crossCurrencyCreditsConvertedAmountAndStoresRate() {
		Account bobUsd = account(21L, bob, USD, "0");
		givenUsersAndAccounts(bobUsd);
		when(currencyConverter.convert(Money.of("80", "UAH"), USD)).thenReturn(new Conversion(
				Money.of("80", "UAH"), Money.of("2", "USD"), new BigDecimal("0.025")));
		when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		TransactionView view = service.transfer("alice", UAH, "bob", USD, new BigDecimal("80"));

		assertThat(bobUsd.getBalance()).isEqualTo(Money.of("2", "USD"));
		assertThat(view.exchangeRate()).isEqualByComparingTo("0.025");
	}

	@Test
	void insufficientFundsLeavesNothingPersisted() {
		givenUsersAndAccounts(bobUah);
		when(currencyConverter.convert(any(), eq(UAH))).thenAnswer(inv -> {
			Money m = inv.getArgument(0);
			return new Conversion(m, m, null);
		});

		BigDecimal tooMuch = new BigDecimal("100.01");
		assertThatThrownBy(() -> service.transfer("alice", UAH, "bob", UAH, tooMuch))
				.isInstanceOf(InsufficientFundsException.class);
		verify(transactionRepository, never()).save(any());
		assertThat(bobUah.getBalance()).isEqualTo(Money.zero(UAH));
	}

	@Test
	void blockedSenderIsRejected() {
		when(userService.getActiveUser("alice"))
				.thenThrow(new ForbiddenOperationException("blocked"));

		assertThatThrownBy(() -> service.transfer("alice", UAH, "bob", UAH, BigDecimal.ONE))
				.isInstanceOf(ForbiddenOperationException.class);
	}

	@Test
	void blockedRecipientIsRejected() {
		when(userService.getActiveUser("alice")).thenReturn(alice);
		bob.changeStatus(UserStatus.BLOCKED);
		when(userService.getByUsername("bob")).thenReturn(bob);

		assertThatThrownBy(() -> service.transfer("alice", UAH, "bob", UAH, BigDecimal.ONE))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("blocked");
	}

	private void givenUsersAndAccounts(Account recipientAccount) {
		when(userService.getActiveUser("alice")).thenReturn(alice);
		when(userService.getByUsername("bob")).thenReturn(bob);
		when(accountService.getFor(alice, UAH)).thenReturn(aliceUah);
		when(accountService.getFor(bob, recipientAccount.getCurrency()))
				.thenReturn(recipientAccount);
	}

	private static User user(long id, String name) {
		User user = new User(name, "hash", Role.USER, NOW);
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private static Account account(long id, User owner, Currency currency, String balance) {
		Account account = new Account(owner, currency, NOW);
		ReflectionTestUtils.setField(account, "id", id);
		Money initial = Money.of(new BigDecimal(balance), currency);
		if (initial.isPositive()) {
			account.credit(initial);
		}
		return account;
	}
}
