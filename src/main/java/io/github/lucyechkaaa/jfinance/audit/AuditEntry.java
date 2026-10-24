package io.github.lucyechkaaa.jfinance.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

/** Immutable audit log record: who did what, to whom, when, and whether it succeeded. */
@Entity
@Immutable
@Table(name = "audit_log")
public class AuditEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	// SQLite AUTOINCREMENT requires the exact column type INTEGER.
	@Column(columnDefinition = "INTEGER")
	private Long id;

	@Column(nullable = false, length = 32)
	private String actor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private AuditAction action;

	@Column(length = 128)
	private String target;

	@Column(length = 512)
	private String details;

	@Column(nullable = false)
	private boolean success;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected AuditEntry() {
		// for JPA
	}

	public AuditEntry(
			String actor,
			AuditAction action,
			String target,
			String details,
			boolean success,
			Instant createdAt) {
		this.actor = Objects.requireNonNull(actor);
		this.action = Objects.requireNonNull(action);
		this.target = target;
		this.details = details;
		this.success = success;
		this.createdAt = Objects.requireNonNull(createdAt);
	}

	public Long getId() {
		return id;
	}

	public String getActor() {
		return actor;
	}

	public AuditAction getAction() {
		return action;
	}

	public String getTarget() {
		return target;
	}

	public String getDetails() {
		return details;
	}

	public boolean isSuccess() {
		return success;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
