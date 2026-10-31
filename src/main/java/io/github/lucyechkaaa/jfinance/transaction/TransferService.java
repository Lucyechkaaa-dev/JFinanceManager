package io.github.lucyechkaaa.jfinance.transaction;

import io.github.lucyechkaaa.jfinance.account.Account;
import io.github.lucyechkaaa.jfinance.account.AccountService;
import io.github.lucyechkaaa.jfinance.audit.AuditAction;
import io.github.lucyechkaaa.jfinance.audit.AuditService;
import io.github.lucyechkaaa.jfinance.common.exception.BusinessRuleException;
import io.github.lucyechkaaa.jfinance.common.money.Money;
import io.github.lucyechkaaa.jfinance.exchange.Conversion;
import io.github.lucyechkaaa.jfinance.exchange.CurrencyConverter;
import io.github.lucyechkaaa.jfinance.transaction.dto.TransactionView;
import io.github.lucyechkaaa.jfinance.user.User;
import io.github.lucyechkaaa.jfinance.user.UserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Currency;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves money between accounts. Each operation runs in a single database transaction:
 * debit, credit, ledger record and audit record are committed together or not at all.
 */
@Service
public class TransferService {

	private final UserService userService;
	private final AccountService accountService;
	private final CurrencyConverter currencyConverter;
	private final TransactionRepository transactionRepository;
	private final AuditService auditService;
	private final Clock clock;

	public TransferService(
			UserService userService,
			AccountService accountService,
			CurrencyConverter currencyConverter,
			TransactionRepository transactionRepository,
			AuditService auditService,
			Clock clock) {
		this.userService = userService;
		this.accountService = accountService;
		this.currencyConverter = currencyConverter;
		this.transactionRepository = transactionRepository;
		this.auditService = auditService;
		this.clock = clock;
	}

	/**
	 * Transfers money to another user, or exchanges currency between the sender's own accounts
	 * when {@code recipientUsername} is the sender.
	 *
	 * @param amount amount to debit, in {@code fromCurrency}
	 */
	@Transactional
	public TransactionView transfer(
			String senderUsername,
			Currency fromCurrency,
			String recipientUsername,
			Currency toCurrency,
			BigDecimal amount) {
		User sender = userService.getActiveUser(senderUsername);
		User recipient = userService.getByUsername(recipientUsername);
		if (!recipient.isActive()) {
			throw new BusinessRuleException(
					"Recipient '%s' is blocked".formatted(recipient.getUsername()));
		}
		Money debit = Money.of(amount, fromCurrency);
		if (!debit.isPositive()) {
			throw new BusinessRuleException("Amount must be positive");
		}

		Account source = accountService.getFor(sender, fromCurrency);
		Account target = accountService.getFor(recipient, toCurrency);
		if (source.getId().equals(target.getId())) {
			throw new BusinessRuleException("Source and target account must differ");
		}
		TransactionType type = sender.getId().equals(recipient.getId())
				? TransactionType.EXCHANGE
				: TransactionType.TRANSFER;

		Conversion conversion = currencyConverter.convert(debit, toCurrency);
		source.debit(debit);
		target.credit(conversion.result());

		FinancialTransaction tx = transactionRepository.save(FinancialTransaction.movement(
				type, source, debit, target, conversion.result(), conversion.rate(),
				sender, clock.instant()));
		auditService.success(sender.getUsername(),
				type == TransactionType.EXCHANGE ? AuditAction.EXCHANGE : AuditAction.TRANSFER,
				"tx:" + tx.getId(),
				"%s -> %s %s%s".formatted(debit, recipient.getUsername(), conversion.result(),
						conversion.isCrossCurrency() ? " @ " + conversion.rate() : ""));
		return TransactionView.from(tx);
	}
}
