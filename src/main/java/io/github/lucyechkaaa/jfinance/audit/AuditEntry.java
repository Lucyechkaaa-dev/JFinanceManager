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

	/** Maximum stored length of {@link #getDetails()}. */
	public static final int MAX_DETAILS_LENGTH = 512;

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

	/** Starts building an audit record. */
	public static Builder builder() {
		return new Builder();
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

	/** Builds {@link AuditEntry} with named steps instead of positional arguments. */
	public static final class Builder {

		private String actor;
		private AuditAction action;
		private String target;
		private String details;
		private Boolean success;
		private Instant createdAt;

		private Builder() {
		}

		/** Who performed the action. */
		public Builder actor(String actor) {
			this.actor = actor;
			return this;
		}

		/** What was done. */
		public Builder action(AuditAction action) {
			this.action = action;
			return this;
		}

		/** Object of the action, e.g. {@code "tx:12"}; optional. */
		public Builder target(String target) {
			this.target = target;
			return this;
		}

		/** Free-text details; optional, truncated to {@link #MAX_DETAILS_LENGTH}. */
		public Builder details(String details) {
			this.details = details;
			return this;
		}

		/** Marks the action as successful. */
		public Builder succeeded() {
			this.success = Boolean.TRUE;
			return this;
		}

		/** Marks the action as refused or failed. */
		public Builder failed() {
			this.success = Boolean.FALSE;
			return this;
		}

		/** When it happened. */
		public Builder at(Instant createdAt) {
			this.createdAt = createdAt;
			return this;
		}

		/**
		 * Creates the record.
		 *
		 * @throws IllegalStateException if actor, action, outcome or time is missing
		 */
		public AuditEntry build() {
			if (actor == null || actor.isBlank()) {
				throw new IllegalStateException("Audit actor is required");
			}
			require(action, "action");
			require(success, "outcome");
			require(createdAt, "time");
			String trimmed = details != null && details.length() > MAX_DETAILS_LENGTH
					? details.substring(0, MAX_DETAILS_LENGTH)
					: details;
			return new AuditEntry(actor, action, target, trimmed, success, createdAt);
		}

		private static void require(Object value, String name) {
			if (value == null) {
				throw new IllegalStateException("Audit " + name + " is required");
			}
		}
	}
}
