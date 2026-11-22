package io.github.lucyechkaaa.jfinance.admin;

import io.github.lucyechkaaa.jfinance.account.dto.AccountResponse;
import io.github.lucyechkaaa.jfinance.admin.dto.AdminRequests.ChangeRoleRequest;
import io.github.lucyechkaaa.jfinance.admin.dto.AdminRequests.ChangeStatusRequest;
import io.github.lucyechkaaa.jfinance.admin.dto.AdminRequests.DepositRequest;
import io.github.lucyechkaaa.jfinance.audit.AuditService;
import io.github.lucyechkaaa.jfinance.audit.dto.AuditEntryResponse;
import io.github.lucyechkaaa.jfinance.transaction.TransactionQueryService;
import io.github.lucyechkaaa.jfinance.transaction.TransactionQueryService.DateRange;
import io.github.lucyechkaaa.jfinance.transaction.dto.TransactionView;
import io.github.lucyechkaaa.jfinance.user.dto.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin API. Every path is protected by {@code hasRole("ADMIN")} in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

	private static final String DEFAULT_AUDIT_LIMIT = "100";
	private static final int MAX_AUDIT_LIMIT = 1000;

	private final AdminService adminService;
	private final TransactionQueryService queryService;
	private final AuditService auditService;
	private final Clock clock;

	public AdminController(
			AdminService adminService,
			TransactionQueryService queryService,
			AuditService auditService,
			Clock clock) {
		this.adminService = adminService;
		this.queryService = queryService;
		this.auditService = auditService;
		this.clock = clock;
	}

	/** Lists all users. */
	@GetMapping("/users")
	public List<UserResponse> users() {
		return adminService.listUsers();
	}

	/** Lists a user's accounts (needed to choose a deposit target). */
	@GetMapping("/users/{userId}/accounts")
	public List<AccountResponse> accounts(@PathVariable long userId) {
		return adminService.accountsOf(userId);
	}

	/** Blocks or unblocks a user. */
	@PatchMapping("/users/{userId}/status")
	public UserResponse changeStatus(
			@PathVariable long userId,
			@Valid @RequestBody ChangeStatusRequest request,
			Principal admin) {
		return adminService.changeStatus(admin.getName(), userId, request.status());
	}

	/** Grants or revokes the admin role. */
	@PatchMapping("/users/{userId}/role")
	public UserResponse changeRole(
			@PathVariable long userId,
			@Valid @RequestBody ChangeRoleRequest request,
			Principal admin) {
		return adminService.changeRole(admin.getName(), userId, request.role());
	}

	/** Deposits money into an account. */
	@PostMapping("/accounts/{accountId}/deposit")
	@ResponseStatus(HttpStatus.CREATED)
	public TransactionView deposit(
			@PathVariable long accountId,
			@Valid @RequestBody DepositRequest request,
			Principal admin) {
		return adminService.deposit(admin.getName(), accountId, request.amount());
	}

	/** Cancels a transaction with a compensating REVERSAL record (storno). */
	@PostMapping("/transactions/{transactionId}/reverse")
	@ResponseStatus(HttpStatus.CREATED)
	public TransactionView reverse(@PathVariable long transactionId, Principal admin) {
		return adminService.reverse(admin.getName(), transactionId);
	}

	/** All ledger records in the date range (default: last 30 days). */
	@GetMapping("/transactions")
	public List<TransactionView> transactions(
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
		return queryService.all(DateRange.of(from, to, LocalDate.now(clock)));
	}

	/** Most recent audit records. */
	@GetMapping("/audit")
	public List<AuditEntryResponse> audit(
			@RequestParam(defaultValue = DEFAULT_AUDIT_LIMIT) @Min(1) @Max(MAX_AUDIT_LIMIT)
			int limit) {
		return auditService.latest(limit).stream().map(AuditEntryResponse::from).toList();
	}
}
