package io.github.lucyechkaaa.jfinance.audit.dto;

import io.github.lucyechkaaa.jfinance.audit.AuditAction;
import io.github.lucyechkaaa.jfinance.audit.AuditEntry;
import java.time.Instant;

/** Public view of an audit record. */
public record AuditEntryResponse(
		Long id, String actor, AuditAction action, String target, String details, boolean success,
		Instant createdAt) {

	/** Maps the entity to its public view. */
	public static AuditEntryResponse from(AuditEntry entry) {
		return new AuditEntryResponse(entry.getId(), entry.getActor(), entry.getAction(),
				entry.getTarget(), entry.getDetails(), entry.isSuccess(), entry.getCreatedAt());
	}
}
