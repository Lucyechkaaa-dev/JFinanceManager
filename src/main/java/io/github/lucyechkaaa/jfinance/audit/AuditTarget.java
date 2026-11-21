package io.github.lucyechkaaa.jfinance.audit;

/** Uniform textual references to audited objects, e.g. {@code "tx:12"}. */
public final class AuditTarget {

	private static final String ACCOUNT_PREFIX = "account:";
	private static final String TRANSACTION_PREFIX = "tx:";

	private AuditTarget() {
	}

	/** Reference to an account. */
	public static String account(long accountId) {
		return ACCOUNT_PREFIX + accountId;
	}

	/** Reference to a ledger transaction. */
	public static String transaction(long transactionId) {
		return TRANSACTION_PREFIX + transactionId;
	}
}
