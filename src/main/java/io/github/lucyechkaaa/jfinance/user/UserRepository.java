package io.github.lucyechkaaa.jfinance.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence port for {@link User}. */
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	boolean existsByUsername(String username);

	List<User> findAllByOrderByIdAsc();
}
