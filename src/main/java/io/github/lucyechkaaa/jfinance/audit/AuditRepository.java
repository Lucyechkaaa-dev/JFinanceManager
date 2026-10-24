package io.github.lucyechkaaa.jfinance.audit;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence port for {@link AuditEntry}. */
public interface AuditRepository extends JpaRepository<AuditEntry, Long> {

	List<AuditEntry> findAllByOrderByIdDesc(Pageable pageable);
}
