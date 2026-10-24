package io.github.lucyechkaaa.jfinance.audit;

/** Security- and money-relevant actions recorded in the audit log. */
public enum AuditAction {
	REGISTER,
	LOGIN,
	LOGIN_FAILED,
	ACCOUNT_LOCKED,
	LOGOUT,
	ACCOUNT_OPENED,
	DEPOSIT,
	TRANSFER,
	EXCHANGE,
	REVERSAL,
	USER_STATUS_CHANGED,
	USER_ROLE_CHANGED,
	ADMIN_BOOTSTRAPPED
}
