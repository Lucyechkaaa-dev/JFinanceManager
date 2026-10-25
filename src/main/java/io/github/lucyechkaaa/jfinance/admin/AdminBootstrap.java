package io.github.lucyechkaaa.jfinance.admin;

import io.github.lucyechkaaa.jfinance.audit.AuditAction;
import io.github.lucyechkaaa.jfinance.audit.AuditService;
import io.github.lucyechkaaa.jfinance.user.Role;
import io.github.lucyechkaaa.jfinance.user.User;
import io.github.lucyechkaaa.jfinance.user.UserRepository;
import io.github.lucyechkaaa.jfinance.user.UserService;
import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first administrator on startup, but only when the system has no users at all.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
	private static final int GENERATED_PASSWORD_BYTES = 12;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final UserRepository userRepository;
	private final UserService userService;
	private final AuditService auditService;
	private final AdminProperties adminProperties;

	public AdminBootstrap(
			UserRepository userRepository,
			UserService userService,
			AuditService auditService,
			AdminProperties adminProperties) {
		this.userRepository = userRepository;
		this.userService = userService;
		this.auditService = auditService;
		this.adminProperties = adminProperties;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (userRepository.count() > 0) {
			log.debug("Users exist, admin bootstrap skipped");
			return;
		}
		boolean generated =
				adminProperties.password() == null || adminProperties.password().isBlank();
		String password = generated ? generatePassword() : adminProperties.password();

		User admin = userService.register(adminProperties.username(), password, Role.ADMIN);
		auditService.success("system", AuditAction.ADMIN_BOOTSTRAPPED, admin.getUsername(), null);

		if (generated) {
			log.warn("No users found. Created admin '{}' with generated password: {} "
					+ "(shown once; set JFINANCE_ADMIN_PASSWORD to choose your own)",
					admin.getUsername(), password);
		} else {
			log.info("No users found. Created admin '{}' from configuration", admin.getUsername());
		}
	}

	private static String generatePassword() {
		byte[] bytes = new byte[GENERATED_PASSWORD_BYTES];
		RANDOM.nextBytes(bytes);
		// URL-safe Base64 of 12 bytes = 16 chars; append a digit+letter to satisfy password policy.
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) + "9a";
	}
}
