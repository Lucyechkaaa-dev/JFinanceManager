package io.github.lucyechkaaa.jfinance.transaction;

import io.github.lucyechkaaa.jfinance.account.Account;
import io.github.lucyechkaaa.jfinance.user.User;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence port for {@link FinancialTransaction}. */
public interface TransactionRepository extends JpaRepository<FinancialTransaction, Long> {

	@Query(
			"""
			select t from FinancialTransaction t
			left join t.sourceAccount s
			left join t.targetAccount d
			where (s.owner = :user or d.owner = :user)
			  and t.createdAt >= :from and t.createdAt < :to
			order by t.createdAt desc, t.id desc
			""")
	List<FinancialTransaction> findForUserBetween(
			@Param("user") User user, @Param("from") Instant from, @Param("to") Instant to);

	@Query(
			"""
			select t from FinancialTransaction t
			where t.createdAt >= :from and t.createdAt < :to
			order by t.createdAt desc, t.id desc
			""")
	List<FinancialTransaction> findAllBetween(
			@Param("from") Instant from, @Param("to") Instant to);

	boolean existsByReversalOf(FinancialTransaction original);

	@Query(
			"""
			select coalesce(sum(t.creditAmountMinor), 0) from FinancialTransaction t
			where t.targetAccount = :account
			""")
	long sumCreditsMinor(@Param("account") Account account);

	@Query(
			"""
			select coalesce(sum(t.debitAmountMinor), 0) from FinancialTransaction t
			where t.sourceAccount = :account
			""")
	long sumDebitsMinor(@Param("account") Account account);
}
