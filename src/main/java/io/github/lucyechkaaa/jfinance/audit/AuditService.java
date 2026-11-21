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

	/** Actor name for actions performed by the system itself (bootstrap, automatic locks). */
	public static final String SYSTEM_ACTOR = "system";

	private static final Logger log = LoggerFactory.getLogger(AuditService.class);

	private final AuditRepository auditRepository;
	private final Clock clock;

	public AuditService(AuditRepository auditRepository, Clock clock) {
		this.auditRepository = auditRepository;
		this.clock = clock;
	}

	/**
	 * Stamps the entry with the current time and stores it.
	 *
	 * @param entry builder with actor, action and outcome set
	 */
	@Transactional
	public void record(AuditEntry.Builder entry) {
		AuditEntry saved = auditRepository.save(entry.at(clock.instant()).build());
		log.info("AUDIT action={} actor={} target={} success={} details={}", saved.getAction(),
				saved.getActor(), saved.getTarget(), saved.isSuccess(), saved.getDetails());
	}

	/** The {@code limit} most recent audit records, newest first. */
	@Transactional(readOnly = true)
	public List<AuditEntry> latest(int limit) {
		return auditRepository.findAllByOrderByIdDesc(PageRequest.ofSize(limit));
	}
}
