package io.github.lucyechkaaa.jfinance.transaction;

import io.github.lucyechkaaa.jfinance.account.Account;
import io.github.lucyechkaaa.jfinance.account.AccountService;
import io.github.lucyechkaaa.jfinance.audit.AuditAction;
import io.github.lucyechkaaa.jfinance.audit.AuditEntry;
import io.github.lucyechkaaa.jfinance.audit.AuditService;
import io.github.lucyechkaaa.jfinance.audit.AuditTarget;
import io.github.lucyechkaaa.jfinance.common.exception.BusinessRuleException;
import io.github.lucyechkaaa.jfinance.common.money.Money;
import io.github.lucyechkaaa.jfinance.exchange.Conversion;
import io.github.lucyechkaaa.jfinance.exchange.CurrencyConverter;
import io.github.lucyechkaaa.jfinance.transaction.dto.TransactionView;
import io.github.lucyechkaaa.jfinance.user.User;
import io.github.lucyechkaaa.jfinance.user.UserService;
import java.time.Clock;
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
	 * when the recipient is the sender.
	 */
	@Transactional
	public TransactionView transfer(TransferCommand command) {
		User sender = userService.getActiveUser(command.senderUsername());
		User recipient = requireActiveRecipient(command.recipientUsername());
		Money debit = requirePositive(Money.of(command.amount(), command.fromCurrency()));
		Account source = accountService.getFor(sender, command.fromCurrency());
		Account target = accountService.getFor(recipient, command.toCurrency());
		if (source.getId().equals(target.getId())) {
			throw new BusinessRuleException("Source and target account must differ");
		}

		Conversion conversion = currencyConverter.convert(debit, command.toCurrency());
		source.debit(debit);
		target.credit(conversion.result());

		TransactionType type = typeOf(sender, recipient);
		FinancialTransaction tx = transactionRepository.save(FinancialTransaction.movement(
				type, source, debit, target, conversion.result(), conversion.rate(),
				command.description(), sender, clock.instant()));
		audit(tx, conversion, recipient);
		return TransactionView.from(tx);
	}

	private User requireActiveRecipient(String recipientUsername) {
		User recipient = userService.getByUsername(recipientUsername);
		if (!recipient.isActive()) {
			throw new BusinessRuleException(
					"Recipient '%s' is blocked".formatted(recipient.getUsername()));
		}
		return recipient;
	}

	private static Money requirePositive(Money amount) {
		if (!amount.isPositive()) {
			throw new BusinessRuleException("Amount must be positive");
		}
		return amount;
	}

	private static TransactionType typeOf(User sender, User recipient) {
		return sender.getId().equals(recipient.getId())
				? TransactionType.EXCHANGE
				: TransactionType.TRANSFER;
	}

	private void audit(FinancialTransaction tx, Conversion conversion, User recipient) {
		AuditAction action = tx.getType() == TransactionType.EXCHANGE
				? AuditAction.EXCHANGE
				: AuditAction.TRANSFER;
		String rate = conversion.isCrossCurrency() ? " @ " + conversion.rate() : "";
		auditService.record(AuditEntry.builder()
				.actor(tx.getInitiatedBy().getUsername())
				.action(action)
				.target(AuditTarget.transaction(tx.getId()))
				.details("%s -> %s %s%s".formatted(conversion.source(), recipient.getUsername(),
						conversion.result(), rate))
				.succeeded());
	}
}
