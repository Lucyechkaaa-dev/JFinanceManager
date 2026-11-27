package io.github.lucyechkaaa.jfinance;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lucyechkaaa.jfinance.account.Account;
import io.github.lucyechkaaa.jfinance.audit.AuditAction;
import io.github.lucyechkaaa.jfinance.audit.AuditEntry;
import io.github.lucyechkaaa.jfinance.common.money.Money;
import io.github.lucyechkaaa.jfinance.support.IntegrationTest;
import io.github.lucyechkaaa.jfinance.transaction.FinancialTransaction;
import io.github.lucyechkaaa.jfinance.transaction.TransactionType;
import io.github.lucyechkaaa.jfinance.user.Role;
import io.github.lucyechkaaa.jfinance.user.User;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Every persisted field is read back from SQLite exactly as it was written: the entity is
 * saved, the persistence context is cleared and the row is loaded again.
 */
class PersistenceRoundTripIntegrationTest extends IntegrationTest {

	private static final Currency UAH = Currency.getInstance("UAH");
	private static final Currency USD = Currency.getInstance("USD");

	@Autowired
	private EntityManager entityManager;
	@Autowired
	private TransactionTemplate transactionTemplate;
	@Autowired
	private Clock clock;

	@Test
	void userIsReadBackUnchanged() {
		Instant now = clock.instant();
		User written = new User(uniqueUsername("rt"), "$argon2id$v=19$hash", Role.ADMIN, now);
		written.registerFailedLogin(1, Duration.ofMinutes(15), now);

		User read = reload(User.class, written, User::getId);

		assertThat(read.getUsername()).isEqualTo(written.getUsername());
		assertThat(read.getPasswordHash()).isEqualTo("$argon2id$v=19$hash");
		assertThat(read.getRole()).isEqualTo(Role.ADMIN);
		assertThat(read.getLockedUntil()).isEqualTo(written.getLockedUntil());
		assertThat(read.getCreatedAt()).isEqualTo(now);
	}

	@Test
	void accountBalanceIsReadBackToTheKopiyka() {
		User owner = persist(new User(uniqueUsername("rt"), "hash", Role.USER, clock.instant()));
		Account written = new Account(owner, USD, clock.instant());
		written.credit(Money.of("9876543210.99", "USD"));

		Account read = reload(Account.class, written, Account::getId);

		assertThat(read.getBalance()).isEqualTo(Money.of("9876543210.99", "USD"));
		assertThat(read.getCurrency()).isEqualTo(USD);
		assertThat(read.getCreatedAt()).isEqualTo(written.getCreatedAt());
	}

	@Test
	void transactionWithAllFieldsIsReadBackUnchanged() {
		User owner = persist(new User(uniqueUsername("rt"), "hash", Role.USER, clock.instant()));
		Account source = persist(new Account(owner, UAH, clock.instant()));
		Account target = persist(new Account(owner, USD, clock.instant()));
		BigDecimal rate = new BigDecimal("0.02216980295479134");
		String description = "Обмін «на відпустку», 100% \"точно\"";
		FinancialTransaction written = FinancialTransaction.movement(TransactionType.EXCHANGE,
				source, Money.of("451.06", "UAH"), target, Money.of("9.99", "USD"),
				rate, description, owner, clock.instant());

		FinancialTransaction read = reload(FinancialTransaction.class, written,
				FinancialTransaction::getId);

		assertThat(read.getType()).isEqualTo(TransactionType.EXCHANGE);
		assertThat(read.getDebit()).contains(Money.of("451.06", "UAH"));
		assertThat(read.getCredit()).contains(Money.of("9.99", "USD"));
		assertThat(read.getExchangeRate()).contains(rate);
		assertThat(read.getDescription()).contains(description);
		assertThat(read.getCreatedAt()).isEqualTo(written.getCreatedAt());
	}

	@Test
	void auditEntryIsReadBackUnchanged() {
		Instant now = clock.instant();
		AuditEntry written = AuditEntry.builder().actor("admin").action(AuditAction.REVERSAL)
				.target("tx:1").details("reversal tx:2").failed().at(now).build();

		AuditEntry read = reload(AuditEntry.class, written, AuditEntry::getId);

		assertThat(read.getAction()).isEqualTo(AuditAction.REVERSAL);
		assertThat(read.getTarget()).isEqualTo("tx:1");
		assertThat(read.getDetails()).isEqualTo("reversal tx:2");
		assertThat(read.isSuccess()).isFalse();
		assertThat(read.getCreatedAt()).isEqualTo(now);
	}

	private <T> T persist(T entity) {
		return transactionTemplate.execute(status -> {
			entityManager.persist(entity);
			return entity;
		});
	}

	/** Saves {@code entity}, forgets every cached instance and loads the row again. */
	private <T> T reload(Class<T> type, T entity, Function<T, Long> idOf) {
		persist(entity);
		return transactionTemplate.execute(status -> {
			entityManager.clear();
			return entityManager.find(type, idOf.apply(entity));
		});
	}
}
