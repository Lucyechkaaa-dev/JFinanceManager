package io.github.lucyechkaaa.jfinance.transaction;

import io.github.lucyechkaaa.jfinance.common.exception.BusinessRuleException;
import io.github.lucyechkaaa.jfinance.transaction.dto.TransactionView;
import io.github.lucyechkaaa.jfinance.user.User;
import io.github.lucyechkaaa.jfinance.user.UserService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only access to the ledger (history, statements). */
@Service
public class TransactionQueryService {

	private final TransactionRepository transactionRepository;
	private final UserService userService;

	public TransactionQueryService(
			TransactionRepository transactionRepository, UserService userService) {
		this.transactionRepository = transactionRepository;
		this.userService = userService;
	}

	/** Transactions touching any of the user's accounts, for the inclusive UTC date range. */
	@Transactional(readOnly = true)
	public List<TransactionView> historyFor(String username, DateRange range) {
		User user = userService.getByUsername(username);
		return transactionRepository.findForUserBetween(user, range.start(), range.endExclusive())
				.stream().map(TransactionView::from).toList();
	}

	/** All ledger records in the range (admin view). */
	@Transactional(readOnly = true)
	public List<TransactionView> all(DateRange range) {
		return transactionRepository.findAllBetween(range.start(), range.endExclusive())
				.stream().map(TransactionView::from).toList();
	}

	/**
	 * Inclusive date range in UTC.
	 *
	 * @param from first day (inclusive)
	 * @param to   last day (inclusive)
	 */
	public record DateRange(LocalDate from, LocalDate to) {

		private static final int DEFAULT_DAYS = 30;

		public DateRange {
			if (from.isAfter(to)) {
				throw new BusinessRuleException("'from' must not be after 'to'");
			}
		}

		/** Builds a range; missing bounds default to the last 30 days ending today. */
		public static DateRange of(LocalDate from, LocalDate to, LocalDate today) {
			LocalDate end = to != null ? to : today;
			return new DateRange(from != null ? from : end.minusDays(DEFAULT_DAYS), end);
		}

		Instant start() {
			return from.atStartOfDay(ZoneOffset.UTC).toInstant();
		}

		Instant endExclusive() {
			return to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		}
	}
}
