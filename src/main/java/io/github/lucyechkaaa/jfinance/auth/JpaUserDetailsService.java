package io.github.lucyechkaaa.jfinance.auth;

import io.github.lucyechkaaa.jfinance.user.UserRepository;
import java.time.Clock;
import java.util.Locale;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads users for Spring Security authentication. */
@Service
public class JpaUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;
	private final Clock clock;

	public JpaUserDetailsService(UserRepository userRepository, Clock clock) {
		this.userRepository = userRepository;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		return userRepository.findByUsername(username.toLowerCase(Locale.ROOT))
				.map(user -> SecurityUser.from(user, clock.instant()))
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}
}
