package io.github.lucyechkaaa.jfinance.audit;

import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes audit records. Joins the caller's transaction, so an audit record of a business
 * action is committed atomically with the action itself. Every record is also mirrored to the
 * application log.
 */
@Service
public class AuditService {

	private static final Logger log = LoggerFactory.getLogger(AuditService.class);
	private static final int MAX_DETAILS_LENGTH = 512;

	private final AuditRepository auditRepository;
	private final Clock clock;

	public AuditService(AuditRepository auditRepository, Clock clock) {
		this.auditRepository = auditRepository;
		this.clock = clock;
	}

	/** Records a successful action. */
	@Transactional
	public void success(String actor, AuditAction action, String target, String details) {
		record(actor, action, target, details, true);
	}

	/** Records a refused or failed action. */
	@Transactional
	public void failure(String actor, AuditAction action, String target, String details) {
		record(actor, action, target, details, false);
	}

	/** The {@code limit} most recent audit records, newest first. */
	@Transactional(readOnly = true)
	public List<AuditEntry> latest(int limit) {
		return auditRepository.findAllByOrderByIdDesc(PageRequest.ofSize(limit));
	}

	private void record(
			String actor, AuditAction action, String target, String details, boolean success) {
		String trimmed = details != null && details.length() > MAX_DETAILS_LENGTH
				? details.substring(0, MAX_DETAILS_LENGTH)
				: details;
		auditRepository.save(
				new AuditEntry(actor, action, target, trimmed, success, clock.instant()));
		log.info("AUDIT action={} actor={} target={} success={} details={}",
				action, actor, target, success, trimmed);
	}
}
