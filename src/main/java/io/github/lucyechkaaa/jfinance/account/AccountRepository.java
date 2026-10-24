package io.github.lucyechkaaa.jfinance.account;

import io.github.lucyechkaaa.jfinance.user.User;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence port for {@link Account}. */
public interface AccountRepository extends JpaRepository<Account, Long> {

	List<Account> findByOwnerOrderByIdAsc(User owner);

	Optional<Account> findByOwnerAndCurrency(User owner, Currency currency);

	boolean existsByOwnerAndCurrency(User owner, Currency currency);
}
