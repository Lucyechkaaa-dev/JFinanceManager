CREATE TABLE users
(
	id                    INTEGER PRIMARY KEY AUTOINCREMENT,
	username              VARCHAR(32)  NOT NULL UNIQUE,
	password_hash         VARCHAR(255) NOT NULL,
	role                  VARCHAR(16)  NOT NULL CHECK (role IN ('USER', 'ADMIN')),
	status                VARCHAR(16)  NOT NULL CHECK (status IN ('ACTIVE', 'BLOCKED')),
	failed_login_attempts INTEGER      NOT NULL DEFAULT 0,
	locked_until          TIMESTAMP,
	created_at            TIMESTAMP    NOT NULL,
	version               BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE accounts
(
	id            INTEGER PRIMARY KEY AUTOINCREMENT,
	user_id       INTEGER    NOT NULL REFERENCES users (id),
	currency      VARCHAR(3) NOT NULL,
	balance_minor BIGINT     NOT NULL DEFAULT 0 CHECK (balance_minor >= 0),
	created_at    TIMESTAMP  NOT NULL,
	version       BIGINT     NOT NULL DEFAULT 0,
	UNIQUE (user_id, currency)
);

-- Append-only ledger: rows are never updated or deleted. Corrections are REVERSAL rows.
CREATE TABLE transactions
(
	id                  INTEGER PRIMARY KEY AUTOINCREMENT,
	type                VARCHAR(16) NOT NULL CHECK (type IN ('DEPOSIT', 'TRANSFER', 'EXCHANGE', 'REVERSAL')),
	source_account_id   INTEGER REFERENCES accounts (id),
	debit_amount_minor  BIGINT CHECK (debit_amount_minor > 0),
	debit_currency      VARCHAR(3),
	target_account_id   INTEGER REFERENCES accounts (id),
	credit_amount_minor BIGINT CHECK (credit_amount_minor > 0),
	credit_currency     VARCHAR(3),
	exchange_rate       VARCHAR(32),
	reversal_of_id      INTEGER UNIQUE REFERENCES transactions (id),
	initiated_by_id     INTEGER     NOT NULL REFERENCES users (id),
	created_at          TIMESTAMP   NOT NULL,
	CHECK (source_account_id IS NOT NULL OR target_account_id IS NOT NULL)
);

CREATE INDEX idx_transactions_source ON transactions (source_account_id);
CREATE INDEX idx_transactions_target ON transactions (target_account_id);
CREATE INDEX idx_transactions_created_at ON transactions (created_at);

CREATE TABLE audit_log
(
	id         INTEGER PRIMARY KEY AUTOINCREMENT,
	actor      VARCHAR(32)  NOT NULL,
	action     VARCHAR(32)  NOT NULL,
	target     VARCHAR(128),
	details    VARCHAR(512),
	success    BOOLEAN      NOT NULL,
	created_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);
