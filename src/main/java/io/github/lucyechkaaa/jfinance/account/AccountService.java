package io.github.lucyechkaaa.jfinance.account;

import io.github.lucyechkaaa.jfinance.audit.AuditAction;
import io.github.lucyechkaaa.jfinance.audit.AuditService;
import io.github.lucyechkaaa.jfinance.common.exception.BusinessRuleException;
import io.github.lucyechkaaa.jfinance.common.exception.ConflictException;
import io.github.lucyechkaaa.jfinance.common.exception.NotFoundException;
import io.github.lucyechkaaa.jfinance.exchange.CurrencyProperties;
import io.github.lucyechkaaa.jfinance.user.User;
import java.time.Clock;
import java.util.Currency;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Opening and looking up currency accounts. */
@Service
public class AccountService {

	private final AccountRepository accountRepository;
	private final CurrencyProperties currencyProperties;
	private final AuditService auditService;
	private final Clock clock;

	public AccountService(
			AccountRepository accountRepository,
			CurrencyProperties currencyProperties,
			AuditService auditService,
			Clock clock) {
		this.accountRepository = accountRepository;
		this.currencyProperties = currencyProperties;
		this.auditService = auditService;
		this.clock = clock;
	}

	/** Opens the account in the base currency that every user gets on registration. */
	@Transactional
	public Account openBaseAccount(User owner) {
		return open(owner, currencyProperties.base());
	}

	/**
	 * Opens a new account in the given currency.
	 *
	 * @throws BusinessRuleException if the currency is not supported
	 * @throws ConflictException     if the user already has an account in this currency
	 */
	@Transactional
	public Account open(User owner, Currency currency) {
		if (!currencyProperties.isSupported(currency)) {
			throw new BusinessRuleException("Currency %s is not supported".formatted(currency));
		}
		if (accountRepository.existsByOwnerAndCurrency(owner, currency)) {
			throw new ConflictException("Account in %s already exists".formatted(currency));
		}
		Account account = accountRepository.save(new Account(owner, currency, clock.instant()));
		auditService.success(owner.getUsername(), AuditAction.ACCOUNT_OPENED,
				"account:" + account.getId(), currency.getCurrencyCode());
		return account;
	}

	/** All accounts of {@code owner}, oldest first. */
	@Transactional(readOnly = true)
	public List<Account> listFor(User owner) {
		return accountRepository.findByOwnerOrderByIdAsc(owner);
	}

	/**
	 * The account of {@code owner} in {@code currency}.
	 *
	 * @throws NotFoundException if the user has no account in this currency
	 */
	@Transactional(readOnly = true)
	public Account getFor(User owner, Currency currency) {
		return accountRepository.findByOwnerAndCurrency(owner, currency)
				.orElseThrow(() -> new NotFoundException("User '%s' has no %s account"
						.formatted(owner.getUsername(), currency)));
	}

	/**
	 * Account by id.
	 *
	 * @throws NotFoundException if it does not exist
	 */
	@Transactional(readOnly = true)
	public Account getById(long accountId) {
		return accountRepository.findById(accountId)
				.orElseThrow(() ->
						new NotFoundException("Account %d not found".formatted(accountId)));
	}
}
