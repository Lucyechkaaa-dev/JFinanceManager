package io.github.lucyechkaaa.jfinance.admin;

import io.github.lucyechkaaa.jfinance.account.Account;
import io.github.lucyechkaaa.jfinance.account.AccountService;
import io.github.lucyechkaaa.jfinance.account.dto.AccountResponse;
import io.github.lucyechkaaa.jfinance.audit.AuditAction;
import io.github.lucyechkaaa.jfinance.audit.AuditEntry;
import io.github.lucyechkaaa.jfinance.audit.AuditService;
import io.github.lucyechkaaa.jfinance.audit.AuditTarget;
import io.github.lucyechkaaa.jfinance.common.exception.BusinessRuleException;
import io.github.lucyechkaaa.jfinance.common.exception.ConflictException;
import io.github.lucyechkaaa.jfinance.common.exception.NotFoundException;
import io.github.lucyechkaaa.jfinance.common.money.Money;
import io.github.lucyechkaaa.jfinance.transaction.FinancialTransaction;
import io.github.lucyechkaaa.jfinance.transaction.TransactionRepository;
import io.github.lucyechkaaa.jfinance.transaction.TransactionType;
import io.github.lucyechkaaa.jfinance.transaction.dto.TransactionView;
import io.github.lucyechkaaa.jfinance.user.Role;
import io.github.lucyechkaaa.jfinance.user.User;
import io.github.lucyechkaaa.jfinance.user.UserRepository;
import io.github.lucyechkaaa.jfinance.user.UserService;
import io.github.lucyechkaaa.jfinance.user.UserStatus;
import io.github.lucyechkaaa.jfinance.user.dto.UserResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Administrative operations. Access is restricted to ADMIN by the security configuration. */
@Service
public class AdminService {

	private final UserRepository userRepository;
	private final UserService userService;
	private final AccountService accountService;
	private final TransactionRepository transactionRepository;
	private final AuditService auditService;
	private final Clock clock;

	public AdminService(
			UserRepository userRepository,
			UserService userService,
			AccountService accountService,
			TransactionRepository transactionRepository,
			AuditService auditService,
			Clock clock) {
		this.userRepository = userRepository;
		this.userService = userService;
		this.accountService = accountService;
		this.transactionRepository = transactionRepository;
		this.auditService = auditService;
		this.clock = clock;
	}

	/** All users, oldest first. */
	@Transactional(readOnly = true)
	public List<UserResponse> listUsers() {
		return userRepository.findAllByOrderByIdAsc().stream().map(UserResponse::from).toList();
	}

	/** Accounts of the given user. */
	@Transactional(readOnly = true)
	public List<AccountResponse> accountsOf(long userId) {
		return accountService.listFor(getUser(userId)).stream().map(AccountResponse::from).toList();
	}

	/** Blocks or unblocks a user. An admin cannot change their own status. */
	@Transactional
	public UserResponse changeStatus(String adminUsername, long userId, UserStatus status) {
		User user = getUser(userId);
		requireNotSelf(adminUsername, user);
		user.changeStatus(status);
		auditService.record(AuditEntry.builder()
				.actor(adminUsername)
				.action(AuditAction.USER_STATUS_CHANGED)
				.target(user.getUsername())
				.details("status=" + status)
				.succeeded());
		return UserResponse.from(user);
	}

	/** Grants or revokes the admin role. An admin cannot change their own role. */
	@Transactional
	public UserResponse changeRole(String adminUsername, long userId, Role role) {
		User user = getUser(userId);
		requireNotSelf(adminUsername, user);
		user.changeRole(role);
		auditService.record(AuditEntry.builder()
				.actor(adminUsername)
				.action(AuditAction.USER_ROLE_CHANGED)
				.target(user.getUsername())
				.details("role=" + role)
				.succeeded());
		return UserResponse.from(user);
	}

	/** Adds money to an account. This is the only way money enters the system. */
	@Transactional
	public TransactionView deposit(String adminUsername, long accountId, BigDecimal amount) {
		Account account = accountService.getById(accountId);
		if (!account.getOwner().isActive()) {
			throw new BusinessRuleException("Account owner is blocked");
		}
		Money money = Money.of(amount, account.getCurrency());
		if (!money.isPositive()) {
			throw new BusinessRuleException("Amount must be positive");
		}
		User admin = userService.getActiveUser(adminUsername);
		account.credit(money);
		FinancialTransaction tx = transactionRepository.save(
				FinancialTransaction.deposit(account, money, admin, clock.instant()));
		auditService.record(AuditEntry.builder()
				.actor(adminUsername)
				.action(AuditAction.DEPOSIT)
				.target(AuditTarget.account(accountId))
				.details(money + " " + AuditTarget.transaction(tx.getId()))
				.succeeded());
		return TransactionView.from(tx);
	}

	/**
	 * Cancels a transaction by recording a compensating REVERSAL (storno); the original record
	 * is never modified. Fails if the money has already been spent from the credited account.
	 */
	@Transactional
	public TransactionView reverse(String adminUsername, long transactionId) {
		User admin = userService.getActiveUser(adminUsername);
		FinancialTransaction original = transactionRepository.findById(transactionId)
				.orElseThrow(() ->
						new NotFoundException("Transaction %d not found".formatted(transactionId)));
		if (original.getType() == TransactionType.REVERSAL) {
			throw new BusinessRuleException("A reversal cannot be reversed");
		}
		if (transactionRepository.existsByReversalOf(original)) {
			throw new ConflictException(
					"Transaction %d is already reversed".formatted(transactionId));
		}

		FinancialTransaction reversal =
				FinancialTransaction.reversalOf(original, admin, clock.instant());
		reversal.getSourceAccount()
				.ifPresent(account -> account.debit(reversal.getDebit().orElseThrow()));
		reversal.getTargetAccount()
				.ifPresent(account -> account.credit(reversal.getCredit().orElseThrow()));
		FinancialTransaction saved = transactionRepository.save(reversal);

		auditService.record(AuditEntry.builder()
				.actor(adminUsername)
				.action(AuditAction.REVERSAL)
				.target(AuditTarget.transaction(transactionId))
				.details("reversal " + AuditTarget.transaction(saved.getId()))
				.succeeded());
		return TransactionView.from(saved);
	}

	private User getUser(long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User %d not found".formatted(userId)));
	}

	private static void requireNotSelf(String adminUsername, User target) {
		if (target.getUsername().equals(adminUsername)) {
			throw new BusinessRuleException(
					"Administrators cannot change their own status or role");
		}
	}
}
